package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

@Component
public class NaturalPKLocalRefBookCreator extends NotVersionedLocalRefBookCreator {

    Logger logger = LoggerFactory.getLogger(NaturalPKLocalRefBookCreator.class);

    public NaturalPKLocalRefBookCreator(
            @Value("${rdm-sync.auto-create.schema:rdm}") String schema,
            @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
            RdmSyncDao dao, SyncSourceDao syncSourceDao,
            VersionMappingService versionMappingService) {
        super(schema, caseIgnore, dao, syncSourceDao, versionMappingService);
    }

    @Override
    protected void createTable(String refBookCode, VersionMapping mapping) {

        String[] split = getTableNameWithSchema(refBookCode, mapping.getTable()).split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        dao.createSchemaIfNotExists(schemaName);
        dao.createTableWithNaturalPrimaryKeyIfNotExists(schemaName, tableName, dao.getFieldMappings(refBookCode), mapping.getDeletedField(), mapping.getPrimaryField());

        logger.info("Preparing table {} in schema {}.", tableName, schemaName);

        dao.addInternalLocalRowStateColumnIfNotExists(schemaName, tableName);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schemaName, tableName);

        logger.info("Table {} in schema {} successfully prepared.", tableName, schemaName);
    }
}
