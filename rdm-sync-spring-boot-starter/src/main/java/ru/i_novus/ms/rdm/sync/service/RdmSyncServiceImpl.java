package ru.i_novus.ms.rdm.sync.service;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.log.LogCriteria;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.util.PageIterator;
import ru.i_novus.ms.rdm.sync.util.RefBookReferenceSort;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.sync.api.model.RowDiffStatus.DELETED;
import static ru.i_novus.ms.rdm.sync.api.model.RowDiffStatus.INSERTED;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@SuppressWarnings({"java:S3740","I-novus:MethodNameWordCountRule"})
public class RdmSyncServiceImpl implements RdmSyncService {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncServiceImpl.class);

    private static final int MAX_SIZE = 100;

    private static final String LOG_NO_MAPPING_FOR_REFBOOK =
            "No version mapping found for reference book with code '{}'.";
    private static final String LOG_ERROR_WHILE_FETCHING_NEW_VERSION =
            "Error while fetching new version with code '%s'.";
    private static final String LOG_ERROR_WHILE_UPDATING_NEW_VERSION =
            "Error while updating new version with code '%s'.";

    private static final String NO_MAPPING_FOR_PRIMARY_KEY = "No mapping found for primary key '%s'.";
    private static final String REFBOOK_WITH_CODE_NOT_FOUND =
            "Reference book with code '%s' not found.";
    private static final String NO_PRIMARY_KEY_FOUND =
            "Reference book with code '%s' has not primary key.";
    private static final String USED_FIELD_IS_DELETED =
            "Field '%s' was deleted in version with code '%s'. Update your mappings.";

    @Autowired
    private Set<SyncSourceService> syncSourceServices;

    @Autowired
    private RdmMappingService mappingService;
    @Autowired
    private RdmLoggingService loggingService;

    @Autowired
    private RdmSyncDao dao;

    private RdmSyncService self;

    @Autowired
    public void setSelf(RdmSyncService self) {
        this.self = self;
    }

    public void setSyncSourceServices(Set<SyncSourceService> syncSourceServices) {
        this.syncSourceServices = syncSourceServices;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {

        List<VersionMapping> versionMappings = dao.getVersionMappings();
        Map<RefBook, SyncSourceService> refBooks = getRefBooksWithSource(versionMappings);
        for (String code : RefBookReferenceSort.getSortedCodes(new ArrayList<>(refBooks.keySet()))) {
            RefBook refBook = refBooks.keySet().stream()
                    .filter(refBookItem -> refBookItem.getCode().equals(code))
                    .findFirst().orElseThrow();
            self.update(
                    refBook,
                    versionMappings.stream()
                            .filter(versionMapping -> versionMapping.getCode().equals(code))
                            .findFirst().orElseThrow(),
                    refBooks.get(refBook)
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update(String refBookCode) {

        if (dao.getVersionMapping(refBookCode) == null) {
            logger.error(LOG_NO_MAPPING_FOR_REFBOOK, refBookCode);
            return;
        }

        Map.Entry<RefBook, SyncSourceService> newVersionWithSource;
        try {
            newVersionWithSource = getLastPublishedVersionWithSource(refBookCode);

        } catch (Exception e) {
            logger.error(String.format(LOG_ERROR_WHILE_FETCHING_NEW_VERSION, refBookCode), e);
            return;
        }

        VersionMapping versionMapping = getVersionMapping(refBookCode);
        try {
            if (isFirstLoad(versionMapping) || isNewVersionPublished(newVersionWithSource.getKey(), versionMapping) || isMappingChanged(versionMapping)) {

                self.update(newVersionWithSource.getKey(), versionMapping, newVersionWithSource.getValue());
                loggingService.logOk(refBookCode, versionMapping.getVersion(), newVersionWithSource.getKey().getLastVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refBookCode);
            }
        } catch (Exception e) {
            logger.error(String.format(LOG_ERROR_WHILE_UPDATING_NEW_VERSION, refBookCode), e);

            loggingService.logError(refBookCode, versionMapping.getVersion(), newVersionWithSource.getKey().getLastVersion(),
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    @Transactional
    public void update(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {

        // Если изменилась структура, проверяем актуальность полей в маппинге
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());
        validateStructureAndMapping(newVersion, fieldMappings);

        dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        try {
            if (isFirstLoad(versionMapping)) {
                //заливаем с нуля
                uploadNew(newVersion, versionMapping, syncSourceService);
                
            } else if (isNewVersionPublished(newVersion, versionMapping)) {
                //если версия и дата публикация не совпадают - нужно обновить справочник
                mergeData(newVersion, versionMapping, syncSourceService, fieldMappings);

            } else if (isMappingChanged(versionMapping)) {
//              Значит в прошлый раз мы синхронизировались по старому маппингу.
//              Необходимо полностью залить свежую версию.
                dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
                uploadNew(newVersion, versionMapping, syncSourceService);
            }
            //обновляем версию в таблице версий клиента
            dao.updateVersionMapping(versionMapping.getId(), newVersion.getLastVersion(), newVersion.getLastPublishDate());

        } finally {
            dao.enableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }
    }

    @Override
    public List<Log> getLog(LogCriteria criteria) {
        return loggingService.getList(criteria.getDate(), criteria.getRefbookCode());
    }

    private boolean isFirstLoad(VersionMapping versionMapping) {
        return versionMapping.getVersion() == null;
    }

    private boolean isNewVersionPublished(RefBook newVersion, VersionMapping versionMapping) {

        return !versionMapping.getVersion().equals(newVersion.getLastVersion())
                && !versionMapping.getPublicationDate().equals(newVersion.getLastPublishDate());
    }

    private boolean isMappingChanged(VersionMapping versionMapping) {
        return versionMapping.changed();
    }

    @Override
    @Transactional(readOnly = true)
    public Response downloadXmlFieldMapping(List<String> refBookCodes) {

        List<VersionMapping> versionMappings = dao.getVersionMappings();
        if (refBookCodes.stream().noneMatch("all"::equalsIgnoreCase)) {
            versionMappings = versionMappings.stream()
                    .filter(mapping -> refBookCodes.contains(mapping.getCode()))
                    .collect(toList());
        }

        XmlMapping xmlMapping = new XmlMapping();
        xmlMapping.setRefbooks(new ArrayList<>());

        for (VersionMapping vm : versionMappings) {
            XmlMappingRefBook xmlMappingRefBook = XmlMappingRefBook.createBy(vm);

            List<XmlMappingField> fields = dao.getFieldMappings(vm.getCode()).stream()
                    .map(XmlMappingField::createBy)
                    .collect(toList());
            xmlMappingRefBook.setFields(fields);
            xmlMapping.getRefbooks().add(xmlMappingRefBook);
        }

        StreamingOutput stream = out -> {
            try {
                Marshaller marshaller = XmlMapping.JAXB_CONTEXT.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(xmlMapping, out);
                out.flush();

            } catch (JAXBException e) {
                throw new RdmException(e); // Не выбросится
            }
        };
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename=\"rdm-mapping.xml\"") .entity(stream).build();
    }

    private VersionMapping getVersionMapping(String refBookCode) {

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode);
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());

        final String primaryField = versionMapping.getPrimaryField();
        if (fieldMappings.stream().noneMatch(mapping -> mapping.getSysField().equals(primaryField)))
            throw new IllegalArgumentException(String.format(NO_MAPPING_FOR_PRIMARY_KEY, primaryField));

        return versionMapping;
    }

    public RefBook getLastPublishedVersion(String refBookCode) {
        return getLastPublishedVersionWithSource(refBookCode).getKey();
    }

    private Map.Entry<RefBook, SyncSourceService> getLastPublishedVersionWithSource(String refBookCode) {

        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);
        refBookCriteria.setCodeExact(refBookCode);
        RefBook refBook = null;
        SyncSourceService resultedSyncSourceService = null;
        for (SyncSourceService syncSourceService : syncSourceServices) {
            refBook = syncSourceService.getRefBook(refBookCode);
            if(refBook != null) {
                resultedSyncSourceService = syncSourceService;
                break;
            }
        }

        if (refBook == null)
            throw new IllegalArgumentException(String.format(REFBOOK_WITH_CODE_NOT_FOUND, refBookCode));

        if (!refBook.getStructure().hasPrimary())
            throw new IllegalStateException(String.format(NO_PRIMARY_KEY_FOUND, refBookCode));

        return Map.entry(refBook, resultedSyncSourceService);
    }

    private Map<RefBook, SyncSourceService> getRefBooksWithSource(List<VersionMapping> versionMappings) {

        Map<RefBook, SyncSourceService> refBooks = new HashMap<>();
        for (VersionMapping versionMapping : versionMappings) {
            try {
                Map.Entry<RefBook, SyncSourceService> lastPublishedVersionWithSource = getLastPublishedVersionWithSource(versionMapping.getCode());
                refBooks.put(lastPublishedVersionWithSource.getKey(), lastPublishedVersionWithSource.getValue());
            } catch (RuntimeException ex) {
                logger.error(String.format(LOG_ERROR_WHILE_FETCHING_NEW_VERSION, versionMapping.getCode()), ex);
                loggingService.logError(versionMapping.getCode(), null, null, ex.getMessage(), ExceptionUtils.getStackTrace(ex));
            }
        }
        return refBooks;
    }

    private void mergeData(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService, List<FieldMapping> fieldMappings) {

        VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(versionMapping.getCode(), newVersion.getLastVersion(), versionMapping.getVersion());
        VersionsDiff diff = syncSourceService.getDiff(versionsDiffCriteria);
        if (diff.isStructureChanged()) {

            dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
            uploadNew(newVersion, versionMapping, syncSourceService);

            return;
        }

        if (!diff.getRows().isEmpty()) {

            PageIterator<RowDiff, VersionsDiffCriteria> iter = new PageIterator<>(
                    criteria -> syncSourceService.getDiff(criteria).getRows(), versionsDiffCriteria, true);
            while (iter.hasNext()) {
                Page<? extends RowDiff> page = iter.next();
                for (RowDiff rowDiff : page.getContent()) {
                    mergeRow(newVersion, rowDiff, versionMapping, fieldMappings);
                }
            }
        }
    }

    private void mergeRow(RefBook newVersion, RowDiff rowDiff,
                          VersionMapping versionMapping, List<FieldMapping> fieldMappings) {

        Map<String, Object> mappedRow = new HashMap<>();
        for (Map.Entry<String, Object> entry : rowDiff.getRow().entrySet()){

            Map<String, Object> mappedValue = mapValue(newVersion,
                    entry.getKey(),
                    entry.getValue(),
                    fieldMappings);

            if (mappedValue != null) {
                mappedRow.putAll(mappedValue);
            }
        }

        final String table = versionMapping.getTable();
        final String primaryField = versionMapping.getPrimaryField();
        final Object primaryValue = mappedRow.get(primaryField);
        boolean idExists = dao.isIdExists(table, primaryField, primaryValue);

        if (DELETED.equals(rowDiff.getStatus())) {
            dao.markDeleted(table, primaryField, versionMapping.getDeletedField(), primaryValue, true, true);

        } else if (INSERTED.equals(rowDiff.getStatus()) && !idExists) {
            dao.insertRow(table, mappedRow, true);

        } else {
            dao.markDeleted(table, primaryField, versionMapping.getDeletedField(), primaryValue, false, true);
            dao.updateRow(table, primaryField, mappedRow, true);
        }
    }

    private Map<String, Object> mapValue(RefBook newVersion, String rdmField, Object value,
                                         List<FieldMapping> fieldMappings) {

        FieldMapping fieldMapping = fieldMappings.stream()
                .filter(mapping -> mapping.getRdmField().equals(rdmField))
                .findAny().orElse(null);
        if (fieldMapping == null)
            return null; // Поле не ведётся в системе

        AttributeTypeEnum attributeType = newVersion.getStructure().getAttributesAndTypes().get(fieldMapping.getRdmField());
        DataTypeEnum clientType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());

        Map<String, Object> mappedValue = new HashMap<>();
        mappedValue.put(fieldMapping.getSysField(), mappingService.map(attributeType, clientType, value));

        return mappedValue;
    }

    private void validateStructureAndMapping(RefBook newVersion, List<FieldMapping> fieldMappings) {

        List<String> clientRdmFields = fieldMappings.stream().map(FieldMapping::getRdmField).collect(toList());
        Set<String> actualFields = newVersion.getStructure().getAttributesAndTypes().keySet();
        if (!actualFields.containsAll(clientRdmFields)) {
            // В новой версии удалены поля, которые ведутся в системе
            clientRdmFields.removeAll(actualFields);
            throw new IllegalStateException(String.format(USED_FIELD_IS_DELETED,
                    String.join(",", clientRdmFields), newVersion.getCode()));
        }

    }

    private void uploadNew(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {

        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());


        final FieldMapping primaryField = fieldMappings.stream()
                .filter(mapping -> mapping.getSysField().equals(versionMapping.getPrimaryField()))
                .findFirst().orElse(null);
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(), primaryField);

        DataCriteria searchDataCriteria = new DataCriteria();
        searchDataCriteria.setCode(versionMapping.getCode());
        searchDataCriteria.setPageSize(MAX_SIZE);

        PageIterator<Map<String, ?>, DataCriteria> iter = new PageIterator<>(
                syncSourceService::getData, searchDataCriteria, true);
        while (iter.hasNext()) {
            Page<? extends Map<String, ?>> page = iter.next();
            for (Map<String, ?> refBookRowValue : page.getContent()) {
                insertOrUpdateRow(refBookRowValue, existingDataIds, versionMapping, fieldMappings, newVersion);
            }
        }
    }

    private void insertOrUpdateRow(Map<String, ?> row, List<Object> existingDataIds,
                                   VersionMapping versionMapping, List<FieldMapping> fieldMappings, RefBook newVersion) {

        final String primaryField = versionMapping.getPrimaryField();

        Map<String, Object> mappedRow = new HashMap<>();
        for (Map.Entry<String, ?> fieldValue : row.entrySet()) {
            Map<String, Object> mappedValue = mapValue(newVersion, fieldValue.getKey(), fieldValue.getValue(), fieldMappings);
            if (mappedValue != null) {
                mappedRow.putAll(mappedValue);
            }
        }

        final Object primaryValue = mappedRow.get(primaryField);
        if (existingDataIds.contains(primaryValue)) {
            // Если запись существует, то обновляем её:
            dao.markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), primaryValue, false, true);
            dao.updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), mappedRow, true);

        } else {
            // Иначе - создаём новую запись:
            dao.insertRow(versionMapping.getTable(), mappedRow, true);
        }
    }
}
