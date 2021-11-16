package ru.i_novus.ms.rdm.sync.service;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.log.LogCriteria;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterServiceLocator;
import ru.i_novus.ms.rdm.sync.util.RefBookReferenceSort;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@SuppressWarnings({"java:S3740", "I-novus:MethodNameWordCountRule"})
public class RdmSyncServiceImpl implements RdmSyncService {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncServiceImpl.class);

    @Value("${rdm.sync.load.size: 1000}")
    private int MAX_SIZE = 1000;

    @Value("${rdm.sync.threads.count:3}")
    private int threadsCount = 3;

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
    private RdmLoggingService loggingService;

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private PersisterServiceLocator persisterServiceLocator;

    @Autowired
    private SyncSourceService syncSourceService;

    private RdmSyncService self;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadsCount);
    }

    @Autowired
    public void setSelf(RdmSyncService self) {
        this.self = self;
    }


    @PreDestroy
    public void destroy() {
        executorService.shutdownNow();
        logger.info("executor was shutdowned");
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update() {

        List<VersionMapping> versionMappings = dao.getVersionMappings();
        List<RefBook> refBooks = getRefBooks(versionMappings);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (String code : RefBookReferenceSort.getSortedCodes(new ArrayList<>(refBooks))) {
            tasks.add(() -> {
                RefBook refBook = refBooks.stream()
                        .filter(refBookItem -> refBookItem.getCode().equals(code))
                        .findFirst().orElseThrow();
                self.update(
                        refBook,
                        versionMappings.stream()
                                .filter(versionMapping -> versionMapping.getCode().equals(code))
                                .findFirst().orElseThrow()
                );
                return null;
            });
            try {
                executorService.invokeAll(tasks);
            } catch (InterruptedException e) {
                logger.info("Interrupted, sync stopping");
                executorService.shutdownNow();
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void update(String refBookCode) {

        if (dao.getVersionMapping(refBookCode, "CURRENT") == null) {
            logger.error(LOG_NO_MAPPING_FOR_REFBOOK, refBookCode);
            return;
        }

        RefBook newVersion;
        try {
            newVersion = getLastPublishedVersion(refBookCode);

        } catch (Exception e) {
            logger.error(String.format(LOG_ERROR_WHILE_FETCHING_NEW_VERSION, refBookCode), e);
            return;
        }

        VersionMapping versionMapping = getVersionMapping(refBookCode);
        LoadedVersion loadedVersion = dao.getLoadedVersion(refBookCode);
        try {
            if (loadedVersion == null || isNewVersionPublished(newVersion, loadedVersion) || isMappingChanged(versionMapping, loadedVersion)) {

                self.update(newVersion, versionMapping);
                loggingService.logOk(refBookCode, versionMapping.getVersion(), newVersion.getLastVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refBookCode);
            }
        } catch (Exception e) {
            logger.error(String.format(LOG_ERROR_WHILE_UPDATING_NEW_VERSION, refBookCode), e);

            loggingService.logError(refBookCode, versionMapping.getVersion(), newVersion.getLastVersion(),
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    @Transactional
    public void update(RefBook newVersion, VersionMapping versionMapping) {

        logger.info("{} sync started", newVersion.getCode());
        // Если изменилась структура, проверяем актуальность полей в маппинге
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());
        validateStructureAndMapping(newVersion, fieldMappings);
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode());

        dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        try {
            PersisterService persisterService = persisterServiceLocator.getPersisterService(versionMapping.getCode());
            if (loadedVersion == null) {
                //заливаем с нуля
                persisterService.firstWrite(newVersion, versionMapping, syncSourceService);

            } else if (isNewVersionPublished(newVersion, loadedVersion)) {
                //если версия и дата публикация не совпадают - нужно обновить справочник
                persisterService.merge(newVersion, loadedVersion.getVersion(), versionMapping, syncSourceService);

            } else if (isMappingChanged(versionMapping, loadedVersion)) {
//              Значит в прошлый раз мы синхронизировались по старому маппингу.
//              Необходимо полностью залить свежую версию.
                persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);
            }
            if (loadedVersion != null) {
                //обновляем версию в таблице версий клиента
                dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
            } else {
                dao.insertLoadedVersion(newVersion.getCode(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
            }
            logger.info("{} sync finished", newVersion.getCode());
        } catch (Exception e) {
            logger.error("cannot sync " + versionMapping.getCode(), e);
        } finally {
            dao.enableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }
    }

    @Override
    public List<Log> getLog(LogCriteria criteria) {
        return loggingService.getList(criteria.getDate(), criteria.getRefbookCode());
    }


    private boolean isNewVersionPublished(RefBook newVersion, LoadedVersion loadedVersion) {

        return !loadedVersion.getVersion().equals(newVersion.getLastVersion())
                && !loadedVersion.getPublicationDate().equals(newVersion.getLastPublishDate());
    }

    private boolean isMappingChanged(VersionMapping versionMapping, LoadedVersion loadedVersion) {
        return versionMapping.getMappingLastUpdated().isAfter(loadedVersion.getLastSync());
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
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename=\"rdm-mapping.xml\"").entity(stream).build();
    }

    @Override
    public RefBook getLastPublishedVersion(String refBookCode) {
        RefBook refBook = syncSourceService.getRefBook(refBookCode);
        if (refBook == null)
            throw new IllegalArgumentException(String.format(REFBOOK_WITH_CODE_NOT_FOUND, refBookCode));

        if (!refBook.getStructure().hasPrimary())
            throw new IllegalStateException(String.format(NO_PRIMARY_KEY_FOUND, refBookCode));
        return refBook;
    }

    private VersionMapping getVersionMapping(String refBookCode) {

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode, "CURRENT");
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());

        final String primaryField = versionMapping.getPrimaryField();
        if (fieldMappings.stream().noneMatch(mapping -> mapping.getSysField().equals(primaryField)))
            throw new IllegalArgumentException(String.format(NO_MAPPING_FOR_PRIMARY_KEY, primaryField));

        return versionMapping;
    }

    private List<RefBook> getRefBooks(List<VersionMapping> versionMappings) {
        List<RefBook> refBooks = new ArrayList<>();
        for (VersionMapping versionMapping : versionMappings) {
            try {
                RefBook lastPublishedVersion = getLastPublishedVersion(versionMapping.getCode());
                refBooks.add(lastPublishedVersion);
            } catch (RuntimeException ex) {
                logger.error(String.format(LOG_ERROR_WHILE_FETCHING_NEW_VERSION, versionMapping.getCode()), ex);
                loggingService.logError(versionMapping.getCode(), null, null, ex.getMessage(), ExceptionUtils.getStackTrace(ex));
            }
        }
        return refBooks;
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
}
