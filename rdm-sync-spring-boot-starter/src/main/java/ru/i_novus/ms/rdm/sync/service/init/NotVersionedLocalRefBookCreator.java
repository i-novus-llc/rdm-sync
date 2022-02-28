package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Set;

@Component
public class NotVersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(NotVersionedLocalRefBookCreator.class);


    public NotVersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                           @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                           RdmSyncDao dao,
                                           SyncSourceDao syncSourceDao) {
        super(schema, caseIgnore, dao, syncSourceDao);

    }

    @Override
    protected void createTable(String refBookCode, VersionMapping mapping) {

        String[] split = getTableNameWithSchema(refBookCode, mapping.getTable()).split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        dao.createSchemaIfNotExists(schemaName);
        dao.createTableIfNotExists(schemaName, tableName, dao.getFieldMappings(mapping.getId()), mapping.getDeletedField(), mapping.getSysPkColumn());

        logger.info("Preparing table {} in schema {}.", tableName, schemaName);

        dao.addInternalLocalRowStateColumnIfNotExists(schemaName, tableName);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schemaName, tableName);

        logger.info("Table {} in schema {} successfully prepared.", tableName, schemaName);
    }
}
