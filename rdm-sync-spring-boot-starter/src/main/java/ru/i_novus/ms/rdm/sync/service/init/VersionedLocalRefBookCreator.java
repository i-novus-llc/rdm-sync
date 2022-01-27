package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Set;

/**
 * пока не используется, предполагался использоваться в более оптимальном хранении версионности
 */
@Component
public class VersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(VersionedLocalRefBookCreator.class);

    private final RdmSyncDao rdmSyncDao;


    public VersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                        @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                        RdmSyncDao rdmSyncDao,
                                        SyncSourceDao syncSourceDao,
                                        Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, caseIgnore, rdmSyncDao, syncSourceDao, syncSourceServiceFactories);

        this.rdmSyncDao = rdmSyncDao;
    }


    @Override
    public void create(String code, String name, String source, SyncTypeEnum type, String table, String range) {

        if(rdmSyncDao.existsLoadedVersion(code)) {
            logger.info("auto create for code {} was skipped", code);
            return;
        }

        logger.info("starting auto create for code {}", code);
        VersionMapping versionMapping = rdmSyncDao.getVersionMapping(code, "CURRENT");
        if (versionMapping == null) {
            RefBookStructure refBookStructure = getRefBookStructure(code, source);

            String schemaTable = getTableNameWithSchema(code, table);
            versionMapping = new VersionMapping(null, code, name, "CURRENT",
                    schemaTable,"someSource", refBookStructure.getPrimaries().get(0), null,
                    null, -1, null, type, range);
            rdmSyncDao.insertVersionMapping(versionMapping);
        }

        createTable(code, versionMapping);

        logger.info("auto create for code {} was finished", code);
    }

    protected void createTable(String code, VersionMapping versionMapping) {
        String[] split = getTableNameWithSchema(code, versionMapping.getTable()).split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        rdmSyncDao.createSchemaIfNotExists(schemaName);
        rdmSyncDao.createVersionedTableIfNotExists(schemaName, tableName, rdmSyncDao.getFieldMappings(versionMapping.getId()));
    }
}
