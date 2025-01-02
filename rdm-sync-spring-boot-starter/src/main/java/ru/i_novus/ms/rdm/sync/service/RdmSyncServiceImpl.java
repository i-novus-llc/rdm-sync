package ru.i_novus.ms.rdm.sync.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.log.LogCriteria;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.xml.XmlMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.xml.XmlMappingField;
import ru.i_novus.ms.rdm.sync.api.mapping.xml.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.downloader.RefBookDownloader;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdater;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdaterException;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdaterLocator;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookVersionsDeterminator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

public class RdmSyncServiceImpl implements RdmSyncService {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncServiceImpl.class);

    @Value("${rdm-sync.threads.count:3}")
    private int threadsCount = 3;

    @Autowired
    private RdmLoggingService loggingService;

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private SyncSourceService syncSourceService;

    @Autowired
    private RefBookDownloader refBookDownloader;

    @Autowired
    private VersionMappingService versionMappingService;

    private ExecutorService executorService;

    @Autowired
    private RefBookUpdaterLocator refBookUpdaterLocator;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadsCount);
    }


    @PreDestroy
    public void destroy() {
        executorService.shutdownNow();
        logger.info("executor was shutdowned");
    }

    @Override
    public void update() {

        List<SyncRefBook> refBooks = dao.getActualSyncRefBooks();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (SyncRefBook refBook : refBooks) {
            tasks.add(() -> {
                update(refBook.getCode());
                return null;
            });
        }
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Interrupted, sync stopping", e);
        }
    }

    @Override
    public void update(String refBookCode) {

        SyncRefBook syncRefBook = dao.getSyncRefBook(refBookCode);
        if (syncRefBook == null) {
            logger.error( "No mapping found for reference book with code '{}'.", refBookCode);
            return;
        }
        List<String> versions = getVersions(refBookCode);
        for (String version : versions) {
            // если не удалось синхронизировать версию, то перестаем дальше синхронизировать остальные версии справочника
            if (!syncVersion(refBookCode, syncRefBook, version)) return;
        }
    }

    private List<String> getVersions(String refBookCode) {
        final RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(refBookCode, dao, syncSourceService, versionMappingService);
        List<String> versions;
        try {
            versions = determinator.getVersions();
        } catch (RefBookUpdaterException e) {
            logger.error("cannot get versions for refbook " + refBookCode, e);
            loggingService.logError(
                    refBookCode,
                    e.getOldVersion(),
                    e.getNewVersion(),
                    e.getCause().getMessage(),
                    ExceptionUtils.getStackTrace(e.getCause())
            );
            versions = Collections.emptyList();
        }
        return versions;
    }

    private boolean syncVersion(String refBookCode, SyncRefBook syncRefBook, String version) {
        DownloadResult downloadResult = null;
        try {
            RefBookUpdater refBookUpdater = refBookUpdaterLocator.getRefBookUpdater(syncRefBook.getType());

            downloadResult = refBookDownloader.download(refBookCode, version);
            refBookUpdater.update(syncSourceService.getRefBook(refBookCode, version), downloadResult);
        } catch (final RefBookUpdaterException e) {
            final Throwable cause = e.getCause();
            logger.error(
                String.format(
                    "Error while updating new version with code '%s'.",
                        refBookCode
                ),
                cause
            );
            loggingService.logError(
                    refBookCode,
                e.getOldVersion(),
                e.getNewVersion(),
                cause.getMessage(),
                ExceptionUtils.getStackTrace(cause)
            );
            return false;
        } catch (Exception e) {
            logger.error("cannot load version {} of refbook {}", version, refBookCode, e);
            LoadedVersion actualLoadedVersion = dao.getActualLoadedVersion(refBookCode);
            loggingService.logError(
                    refBookCode,
                    actualLoadedVersion != null ? actualLoadedVersion.getVersion() : null,
                    version,
                    e.getMessage(),
                    ExceptionUtils.getStackTrace(e)
            );
            return false;
        } finally {
            if (Objects.nonNull(downloadResult) && Objects.nonNull(downloadResult.getTableName()))
                dao.dropTable(downloadResult.getTableName());
        }
        return true;
    }

    @Override
    public List<Log> getLog(LogCriteria criteria) {
        return loggingService.getList(criteria.getDate(), criteria.getRefbookCode());
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

            List<XmlMappingField> fields = dao.getFieldMappings(vm.getId()).stream()
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
                throw new RdmSyncException(e); // Не выбросится
            }
        };
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename=\"rdm-mapping.xml\"").entity(stream).build();
    }
}
