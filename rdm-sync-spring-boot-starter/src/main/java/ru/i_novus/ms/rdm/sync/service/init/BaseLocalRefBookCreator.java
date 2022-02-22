package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.Set;

public abstract class BaseLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(BaseLocalRefBookCreator.class);

    protected static final String LOG_AUTOCREATE_SKIP =
            "Skip autocreation of mapping data from structure of RefBook with code '{}'.";
    protected static final String LOG_AUTOCREATE_START =
            "Autocreation mapping data from structure of RefBook with code '{}' is started.";

    private final String defaultSchema;

    protected final boolean caseIgnore;

    protected final SyncSourceDao syncSourceDao;

    protected final RdmSyncDao dao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    protected abstract void createTable(String refBookCode, VersionMapping mapping);

    public BaseLocalRefBookCreator(String defaultSchema,
                                   Boolean caseIgnore,
                                   RdmSyncDao dao,
                                   SyncSourceDao syncSourceDao,
                                   Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        this.defaultSchema = defaultSchema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);

        this.syncSourceDao = syncSourceDao;
        this.dao = dao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
    }

    @Transactional
    @Override
    public void create(VersionAndFieldMapping versionAndFieldMapping) {
        String refBookCode = versionAndFieldMapping.getVersionMapping().getCode();

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode, "CURRENT");
        if (versionMapping != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
        } else {
            logger.info(LOG_AUTOCREATE_START, refBookCode);
            versionMapping = saveMapping(versionAndFieldMapping.getVersionMapping(), versionAndFieldMapping.getFieldMapping());
        }
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        if (versionMapping != null) {
            createTable(refBookCode, versionMapping);
        }
    }

    protected VersionMapping saveMapping(VersionMapping vm, List<FieldMapping> fm) {
        String refBookCode = vm.getCode();
        String refBookVersion = vm.getRefBookVersion();
        VersionMapping modifiedVersionMapping = modifyVersionMappingForDifferentCreator(vm);
        VersionMapping currentVersionMapping = dao.getVersionMapping(refBookCode, refBookVersion);
        if (currentVersionMapping == null) {
            Integer mappingId = dao.insertVersionMapping(modifiedVersionMapping);
            dao.insertFieldMapping(mappingId, fm);
            logger.info("mapping for code {} with version {} was added", refBookCode, refBookVersion);
            modifiedVersionMapping.setId(mappingId);
        } else if (modifiedVersionMapping.getMappingVersion() > currentVersionMapping.getMappingVersion()) {
            logger.info("load {}", refBookCode);
            dao.updateCurrentMapping(modifiedVersionMapping);
            dao.insertFieldMapping(currentVersionMapping.getMappingId(), fm);
            logger.info("mapping for code {} with version {} was updated", refBookCode, modifiedVersionMapping.getMappingVersion());
        } else {
            logger.info("mapping for {} not changed", refBookCode);
        }
        return modifiedVersionMapping;
    }

    public String getTableNameWithSchema(String refBookCode, String refBookTable) {
        return RdmSyncInitUtils.buildTableNameWithSchema(refBookCode, refBookTable, defaultSchema, caseIgnore);
    }

    protected abstract VersionMapping modifyVersionMappingForDifferentCreator(VersionMapping vm);

}
