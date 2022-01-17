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
public class SimpleVersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(SimpleVersionedLocalRefBookCreator.class);

    public SimpleVersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                              @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                              RdmSyncDao rdmSyncDao,
                                              SyncSourceDao syncSourceDao,
                                              Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, caseIgnore, rdmSyncDao, syncSourceDao, syncSourceServiceFactories);

    }

    @Override
    protected void createTable(String code, VersionMapping versionMapping) {

        String[] split = versionMapping.getTable().split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        dao.createSchemaIfNotExists(schema);
        dao.createSimpleVersionedTables(schemaName, tableName, dao.getFieldMappings(code));
    }
}
