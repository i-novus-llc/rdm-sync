package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

import java.util.Set;

public abstract class BaseLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(BaseLocalRefBookCreator.class);

    private static final String LOG_AUTOCREATE_ERROR =
            "Error autocreation mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_LAST_PUBLISHED_NOT_FOUND = " Can't get last published version from RDM.";

    protected final String schema;
    protected final boolean caseIgnore;

    private final SyncSourceDao syncSourceDao;


    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    public BaseLocalRefBookCreator(String schema,
                                   Boolean caseIgnore,
                                   SyncSourceDao syncSourceDao,
                                   Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        this.schema = schema == null ? "rdm" : schema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);

        this.syncSourceDao = syncSourceDao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
    }

    protected String getTableName(String refBookCode, String refBookTable) {

        String schemaName;
        String tableName;

        if (!StringUtils.isEmpty(refBookTable)) {

            String[] split = refBookTable.split("\\.");
            schemaName = (split.length > 1) ? split[0] : schema;
            tableName = (split.length > 1) ? split[1] : refBookTable;

        } else {
            schemaName = schema;
            tableName = refBookCode.replaceAll("[-.]", "_");
            tableName = "ref_" + (caseIgnore ? tableName.toLowerCase() : tableName);
        }
        
        return String.format("%s.%s", schemaName, tableName);
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
