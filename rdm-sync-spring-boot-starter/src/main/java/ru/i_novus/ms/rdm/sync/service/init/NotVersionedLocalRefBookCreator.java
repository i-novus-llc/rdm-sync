package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;

@Component
public class NotVersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(NotVersionedLocalRefBookCreator.class);


    public NotVersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                           @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                           RdmSyncDao dao,
                                           SyncSourceDao syncSourceDao,
                                           VersionMappingService versionMappingService) {
        super(schema, caseIgnore, dao, syncSourceDao, versionMappingService);

    }

    @Override
    protected void createTable(String schemaName, String tableName, VersionMapping mapping, List<FieldMapping> fieldMappings) {

        dao.createSchemaIfNotExists(schemaName);
        dao.createNotVersionedTableIfNotExists(schemaName, tableName, fieldMappings, mapping.getDeletedField(), mapping.getSysPkColumn(), mapping.getPrimaryField());

        logger.info("Preparing table {} in schema {}.", tableName, schemaName);

        dao.addInternalLocalRowStateColumnIfNotExists(schemaName, tableName);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schemaName, tableName);

        logger.info("Table {} in schema {} successfully prepared.", tableName, schemaName);
    }
}
