package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

import java.util.Set;

public abstract class BaseLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(BaseLocalRefBookCreator.class);

    private static final String LOG_AUTOCREATE_ERROR =
            "Error autocreation mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_LAST_PUBLISHED_NOT_FOUND = " Can't get last published version from RDM.";

    protected final String schema;

    private final SyncSourceDao syncSourceDao;


    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    public BaseLocalRefBookCreator(String schema, SyncSourceDao syncSourceDao, Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        this.schema = schema == null ? "rdm" : schema;
        this.syncSourceDao = syncSourceDao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
    }

    protected String getTableName(String refBookCode) {
        return String.format("%s.%s", schema, "ref_" + refBookCode.replaceAll("[-.]", "_").toLowerCase());
    }

    protected RefBookStructure getRefBookStructure(String refBookCode, String source) {
        RefBook lastPublished;
        try {
            lastPublished = getSyncSourceService(source).getRefBook(refBookCode);

        } catch (Exception e) {
            logger.error(LOG_AUTOCREATE_ERROR + LOG_LAST_PUBLISHED_NOT_FOUND, refBookCode, e);
            return null;
        }

        return lastPublished.getStructure();
    }

    protected SyncSourceService getSyncSourceService(String sourceCode) {
        SyncSource source = syncSourceDao.findByCode(sourceCode);
        return syncSourceServiceFactories
                .stream()
                .filter(factory -> factory.isSatisfied(source))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("cannot find factory by " + source.getFactoryName()))
                .createService(source);
    }

}
