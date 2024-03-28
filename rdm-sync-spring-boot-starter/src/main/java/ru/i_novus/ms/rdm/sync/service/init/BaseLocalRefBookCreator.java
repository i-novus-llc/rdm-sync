package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;

public abstract class BaseLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(BaseLocalRefBookCreator.class);


    private final String defaultSchema;

    protected final boolean caseIgnore;

    protected final SyncSourceDao syncSourceDao;

    protected final RdmSyncDao dao;


    protected abstract void createTable(String refBookCode, VersionMapping mapping);

    protected BaseLocalRefBookCreator(String defaultSchema,
                                   Boolean caseIgnore,
                                   RdmSyncDao dao,
                                   SyncSourceDao syncSourceDao) {
        this.defaultSchema = defaultSchema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);
        this.syncSourceDao = syncSourceDao;
        this.dao = dao;
    }

    @Transactional
    @Override
    public void create(SyncMapping syncMapping) {
        String refBookCode = syncMapping.getVersionMapping().getCode();

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode, syncMapping.getVersionMapping().getRefBookVersion());
        saveMapping(syncMapping.getVersionMapping(), syncMapping.getFieldMapping(), versionMapping);

        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        if (versionMapping == null) {
            createTable(refBookCode, syncMapping.getVersionMapping());
        }
    }

    protected void saveMapping(VersionMapping newVersionMapping, List<FieldMapping> fm, VersionMapping oldVersionMapping) {
        String refBookCode = newVersionMapping.getCode();
        String refBookVersion = newVersionMapping.getRefBookVersion();

        if (oldVersionMapping == null) {
            Integer mappingId = dao.insertVersionMapping(newVersionMapping);
            dao.insertFieldMapping(mappingId, fm);
            logger.info("mapping for code {} with version {} was added", refBookCode, refBookVersion);
            newVersionMapping.setId(mappingId);
        } else if (newVersionMapping.getMappingVersion() > oldVersionMapping.getMappingVersion()) {
            logger.info("load {}", refBookCode);
            dao.updateCurrentMapping(newVersionMapping);
            dao.insertFieldMapping(oldVersionMapping.getMappingId(), fm);
            logger.info("mapping for code {} with version {} was updated", refBookCode, newVersionMapping.getMappingVersion());
        } else {
            logger.info("mapping for {} not changed", refBookCode);
        }
    }

    public String getTableNameWithSchema(String refBookCode, String refBookTable) {
        return RdmSyncInitUtils.buildTableNameWithSchema(refBookCode, refBookTable, defaultSchema, caseIgnore);
    }
}
