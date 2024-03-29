package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

/**
 * пока не используется, предполагался использоваться в более оптимальном хранении версионности
 */
@Component
public class SimpleVersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(SimpleVersionedLocalRefBookCreator.class);

    public SimpleVersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                              @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                              RdmSyncDao rdmSyncDao,
                                              SyncSourceDao syncSourceDao) {
        super(schema, caseIgnore, rdmSyncDao, syncSourceDao);

    }
    @Override
    protected void createTable(String code, VersionMapping versionMapping) {

        String[] split = getTableNameWithSchema(code, versionMapping.getTable()).split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        dao.createSchemaIfNotExists(schemaName);
        dao.createSimpleVersionedTable(schemaName, tableName, dao.getFieldMappings(versionMapping.getId()), versionMapping.getPrimaryField());
    }
}
