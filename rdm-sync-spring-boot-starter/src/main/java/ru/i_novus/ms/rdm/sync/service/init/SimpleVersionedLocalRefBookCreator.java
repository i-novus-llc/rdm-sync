package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;


@Component
public class SimpleVersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    public SimpleVersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                              @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                              RdmSyncDao rdmSyncDao,
                                              SyncSourceDao syncSourceDao,
                                              VersionMappingService versionMappingService) {
        super(schema, caseIgnore, rdmSyncDao, syncSourceDao, versionMappingService);

    }
    @Override
    protected void createTable(String schemaName, String tableName, VersionMapping versionMapping, List<FieldMapping> fieldMappings) {

        dao.createSchemaIfNotExists(schemaName);
        dao.createSimpleVersionedTable(schemaName, tableName, fieldMappings, versionMapping.getPrimaryField());
    }
}