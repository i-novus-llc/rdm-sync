package ru.i_novus.ms.rdm.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.log.LogCriteria;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdater;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdaterLocator;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookVersionIterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.ArrayList;
import java.util.List;
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

    @Value("${rdm-sync.load.size: 1000}")
    private int MAX_SIZE = 1000;

    @Value("${rdm-sync.threads.count:3}")
    private int threadsCount = 3;

    private static final String LOG_NO_MAPPING_FOR_REFBOOK =
            "No mapping found for reference book with code '{}'.";

    @Autowired
    private RdmLoggingService loggingService;

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private SyncSourceService syncSourceService;


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

        List<VersionMapping> versionMappings = dao.getVersionMappings();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (VersionMapping mapping : versionMappings) {
            tasks.add(() -> {
                update(mapping.getCode());
                return null;
            });
        }
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            logger.info("Interrupted, sync stopping");
            executorService.shutdownNow();
        }
    }

    @Override
    public void update(String refBookCode) {

        SyncRefBook syncRefBook = dao.getSyncRefBook(refBookCode);
        if (syncRefBook == null) {
            logger.error(LOG_NO_MAPPING_FOR_REFBOOK, refBookCode);
            return;
        }

        RefBookUpdater refBookUpdater = refBookUpdaterLocator.getRefBookUpdater(syncRefBook.getType());
        if(syncRefBook.getRange() != null) {
           new RefBookVersionIterator(syncRefBook, dao, syncSourceService).forEachRemaining(version -> refBookUpdater.update(refBookCode, version));
        } else {
            refBookUpdater.update(refBookCode, null);
        }
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
                throw new RdmException(e); // Не выбросится
            }
        };
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "filename=\"rdm-mapping.xml\"").entity(stream).build();
    }
}
