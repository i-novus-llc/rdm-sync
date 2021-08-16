package ru.i_novus.ms.rdm.sync.service;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.log.LogCriteria;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.util.RefBookReferenceSort;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.DELETED;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.INSERTED;

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
    private static final String SEVERAL_REFBOOKS_WITH_CODE_FOUND =
            "Several reference books with code '%s' found.";
    private static final String NO_PRIMARY_KEY_FOUND =
            "Reference book with code '%s' has not primary key.";
    private static final String MULTIPLE_PRIMARY_KEYS_FOUND =
            "Reference book with code '%s' has multiple primary keys.";
    private static final String USED_FIELD_IS_DELETED =
            "Field '%s' was deleted in version with code '%s'. Update your mappings.";

    @Autowired
    private RefBookService refBookService;
    @Autowired
    private VersionRestService versionService;
    @Autowired
    private CompareService compareService;

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

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {

        List<VersionMapping> versionMappings = dao.getVersionMappings();
        List<RefBook> refBooks = getRefBooks(versionMappings);
        for (String code : RefBookReferenceSort.getSortedCodes(refBooks)) {
            self.update(
                refBooks.stream()
                        .filter(refBook -> refBook.getCode().equals(code))
                        .findFirst().orElseThrow(),
                versionMappings.stream()
                        .filter(versionMapping -> versionMapping.getCode().equals(code))
                        .findFirst().orElseThrow()
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

        RefBook newVersion;
        try {
            newVersion = getLastPublishedVersionFromRdm(refBookCode);

        } catch (Exception e) {
            logger.error(String.format(LOG_ERROR_WHILE_FETCHING_NEW_VERSION, refBookCode), e);
            return;
        }

        VersionMapping versionMapping = getVersionMapping(refBookCode);
        try {
            if (isFirstLoad(versionMapping) || isNewVersionPublished(newVersion, versionMapping) || isMappingChanged(versionMapping)) {

                self.update(newVersion, versionMapping);
                loggingService.logOk(refBookCode, versionMapping.getVersion(), newVersion.getLastPublishedVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refBookCode);
            }
        } catch (Exception e) {
            logger.error(String.format(LOG_ERROR_WHILE_UPDATING_NEW_VERSION, refBookCode), e);

            loggingService.logError(refBookCode, versionMapping.getVersion(), newVersion.getLastPublishedVersion(),
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    @Transactional
    public void update(RefBook newVersion, VersionMapping versionMapping) {

        dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        try {
            if (isFirstLoad(versionMapping)) {
                //заливаем с нуля
                uploadNew(newVersion, versionMapping);
                
            } else if (isNewVersionPublished(newVersion, versionMapping)) {
                //если версия и дата публикация не совпадают - нужно обновить справочник
                mergeData(newVersion, versionMapping);

            } else if (isMappingChanged(versionMapping)) {
//              Значит в прошлый раз мы синхронизировались по старому маппингу.
//              Необходимо полностью залить свежую версию.
                dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
                uploadNew(newVersion, versionMapping);
            }
            //обновляем версию в таблице версий клиента
            dao.updateVersionMapping(versionMapping.getId(), newVersion.getLastPublishedVersion(), newVersion.getLastPublishedVersionFromDate());

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

        return !versionMapping.getVersion().equals(newVersion.getLastPublishedVersion())
                && !versionMapping.getPublicationDate().equals(newVersion.getLastPublishedVersionFromDate());
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
                    .filter(vm -> refBookCodes.contains(vm.getCode()))
                    .collect(toList());
        }

        XmlMapping xmlMapping = new XmlMapping();
        xmlMapping.setRefbooks(new ArrayList<>());

        for (VersionMapping vm : versionMappings) {
            XmlMappingRefBook xmlMappingRefBook = XmlMappingRefBook.createBy(vm);
            xmlMappingRefBook.setFields(dao.getFieldMapping(vm.getCode()).stream().map(XmlMappingField::createBy).collect(toList()));
            xmlMapping.getRefbooks().add(xmlMappingRefBook);
        }

        StreamingOutput stream = out -> {
            try {
                Marshaller marshaller = XmlMapping.JAXB_CONTEXT.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(xmlMapping, out);
                out.flush();
            } catch (JAXBException e) {
//              Не выбросится
                throw new RdmException(e);
            }
        };
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename=\"rdm-mapping.xml\"") .entity(stream).build();
    }

    private VersionMapping getVersionMapping(String refBookCode) {

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode);
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());

        final String primaryField = versionMapping.getPrimaryField();
        if (fieldMappings.stream().noneMatch(f -> f.getSysField().equals(primaryField)))
            throw new IllegalArgumentException(String.format(NO_MAPPING_FOR_PRIMARY_KEY, primaryField));

        return versionMapping;
    }

    public RefBook getLastPublishedVersionFromRdm(String refBookCode) {

        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);
        refBookCriteria.setCodeExact(refBookCode);

        Page<RefBook> page = refBookService.search(refBookCriteria);
        if (page.getContent().isEmpty())
            throw new IllegalArgumentException(String.format(REFBOOK_WITH_CODE_NOT_FOUND, refBookCode));
        if (page.getContent().size() > 1)
            throw new IllegalStateException(String.format(SEVERAL_REFBOOKS_WITH_CODE_FOUND, refBookCode));

        RefBook last = page.getContent().iterator().next();
        if (last.getStructure().getPrimaries().isEmpty())
            throw new IllegalStateException(String.format(NO_PRIMARY_KEY_FOUND, refBookCode));
        if (last.getStructure().getPrimaries().size() > 1)
            throw new UnsupportedOperationException(String.format(MULTIPLE_PRIMARY_KEYS_FOUND, refBookCode));

        return last;
    }

    private List<RefBook> getRefBooks(List<VersionMapping> versionMappings) {

        List<RefBook> refBooks = new ArrayList<>();
        for (VersionMapping versionMapping : versionMappings) {
            try {
                refBooks.add(getLastPublishedVersionFromRdm(versionMapping.getCode()));
            } catch (RuntimeException ex) {
                logger.error(String.format(LOG_ERROR_WHILE_FETCHING_NEW_VERSION, versionMapping.getCode()), ex);
                loggingService.logError(versionMapping.getCode(), null, null, ex.getMessage(), ExceptionUtils.getStackTrace(ex));
            }
        }
        return refBooks;
    }

    private void mergeData(RefBook newVersion, VersionMapping versionMapping) {

        Integer oldVersionId = versionService.getVersion(versionMapping.getVersion(), versionMapping.getCode()).getId();
        StructureDiff structureDiff = compareService.compareStructures(oldVersionId, newVersion.getId());
        if (!CollectionUtils.isEmpty(structureDiff.getUpdated()) || !CollectionUtils.isEmpty(structureDiff.getDeleted()) || !CollectionUtils.isEmpty(structureDiff.getInserted())) {
            dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
            uploadNew(newVersion, versionMapping);
            return;
        }
        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());

        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(oldVersionId);
        compareDataCriteria.setNewVersionId(newVersion.getId());
        compareDataCriteria.setCountOnly(true);
        compareDataCriteria.setPageSize(1);
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);

        // Если изменилась структура, проверяем актуальность полей в маппинге
        validateStructureChanges(versionMapping, fieldMappings, diff);
        if (diff.getRows().getTotalElements() > 0) {
            compareDataCriteria.setCountOnly(false);
            compareDataCriteria.setPageSize(MAX_SIZE);
            PageIterator<DiffRowValue, CompareDataCriteria> iter = new PageIterator<>(
                    criteria -> compareService.compareData(criteria).getRows(), compareDataCriteria, true);
            while (iter.hasNext()) {
                Page<? extends DiffRowValue> page = iter.next();
                for (DiffRowValue diffRowValue : page.getContent()) {
                    mergeRow(newVersion, diffRowValue, versionMapping, fieldMappings);
                }
            }
        }
    }

    private void mergeRow(RefBook newVersion, DiffRowValue row,
                          VersionMapping versionMapping, List<FieldMapping> fieldMappings) {

        Map<String, Object> mappedRow = new HashMap<>();
        for (DiffFieldValue diffFieldValue : row.getValues()) {

            Map<String, Object> mappedValue = mapValue(newVersion,
                    diffFieldValue.getField().getName(),
                    getDiffFieldValue(diffFieldValue, row.getStatus()),
                    fieldMappings);

            if (mappedValue != null) {
                mappedRow.putAll(mappedValue);
            }
        }

        final String table = versionMapping.getTable();
        final String primaryField = versionMapping.getPrimaryField();
        final Object primaryValue = mappedRow.get(primaryField);
        boolean idExists = dao.isIdExists(table, primaryField, primaryValue);

        if (DELETED.equals(row.getStatus())) {
            dao.markDeleted(table, primaryField, versionMapping.getDeletedField(), primaryValue, true, true);

        } else if (INSERTED.equals(row.getStatus()) && !idExists) {
            dao.insertRow(table, mappedRow, true);

        } else {
            dao.markDeleted(table, primaryField, versionMapping.getDeletedField(), primaryValue, false, true);
            dao.updateRow(table, primaryField, mappedRow, true);
        }
    }

    private Object getDiffFieldValue(DiffFieldValue fieldValue, DiffStatusEnum status) {

        return DiffStatusEnum.DELETED.equals(status) ? fieldValue.getOldValue() : fieldValue.getNewValue();
    }

    private Map<String, Object> mapValue(RefBook newVersion, String rdmField, Object value,
                                         List<FieldMapping> fieldMappings) {

        FieldMapping fieldMapping = fieldMappings.stream()
                .filter(mapping -> mapping.getRdmField().equals(rdmField))
                .findAny().orElse(null);
        if (fieldMapping == null)
            return null; // Поле не ведётся в системе

        FieldType rdmType = newVersion.getStructure().getAttribute(fieldMapping.getRdmField()).getType();
        DataTypeEnum clientType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());

        Map<String, Object> mappedValue = new HashMap<>();
        mappedValue.put(fieldMapping.getSysField(), mappingService.map(rdmType, clientType, value));

        return mappedValue;
    }

    private void validateStructureChanges(VersionMapping versionMapping, List<FieldMapping> fieldMappings,
                                          RefBookDataDiff diff) {

        List<String> clientRdmFields = fieldMappings.stream().map(FieldMapping::getRdmField).collect(toList());

        // Проверка удалённых полей
        List<String> oldAttributes = diff.getAttributeDiff().getOldAttributes();
        if (!CollectionUtils.isEmpty(oldAttributes)) {
            oldAttributes.retainAll(clientRdmFields);
            if (!oldAttributes.isEmpty()) {
                // В новой версии удалены поля, которые ведутся в системе
                throw new IllegalStateException(String.format(USED_FIELD_IS_DELETED,
                        String.join(",", oldAttributes), versionMapping.getCode()));
            }
        }
    }

    private void uploadNew(RefBook newVersion, VersionMapping versionMapping) {

        List<FieldMapping> fieldMappings = dao.getFieldMapping(versionMapping.getCode());


        final FieldMapping primaryField = fieldMappings.stream()
                .filter(mapping -> mapping.getSysField().equals(versionMapping.getPrimaryField()))
                .findFirst().orElse(null);
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(), primaryField);

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageSize(MAX_SIZE);

        PageIterator<RefBookRowValue, SearchDataCriteria> iter = new PageIterator<>(
                criteria -> versionService.search(versionMapping.getCode(), criteria), searchDataCriteria, true);
        while (iter.hasNext()) {
            Page<? extends RefBookRowValue> page = iter.next();
            for (RefBookRowValue refBookRowValue : page.getContent()) {
                insertOrUpdateRow(refBookRowValue, existingDataIds, versionMapping, fieldMappings, newVersion);
            }
        }
    }

    private void insertOrUpdateRow(RefBookRowValue row, List<Object> existingDataIds,
                                   VersionMapping versionMapping, List<FieldMapping> fieldMappings, RefBook newVersion) {

        final String primaryField = versionMapping.getPrimaryField();

        Map<String, Object> mappedRow = new HashMap<>();
        for (FieldValue fieldValue : row.getFieldValues()) {
            Map<String, Object> mappedValue = mapValue(newVersion, fieldValue.getField(), fieldValue.getValue(), fieldMappings);
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
