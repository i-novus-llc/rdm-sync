package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

import java.util.*;

@Component
public class NotVersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(NotVersionedLocalRefBookCreator.class);


    public NotVersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                           @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                           RdmSyncDao dao,
                                           SyncSourceDao syncSourceDao,
                                           Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, caseIgnore, dao, syncSourceDao, syncSourceServiceFactories);

    }

    protected void createTable(String refBookCode, VersionMapping mapping) {

        String[] split = mapping.getTable().split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        dao.createSchemaIfNotExists(schemaName);
        dao.createTableIfNotExists(schemaName, tableName, dao.getFieldMappings(mapping.getId()), mapping.getDeletedField());

        logger.info("Preparing table {} in schema {}.", tableName, schemaName);

        dao.addInternalLocalRowStateColumnIfNotExists(schema, tableName);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, tableName);

        logger.info("Table {} in schema {} successfully prepared.", tableName, schemaName);
    }

    @Override
    protected VersionMapping getVersionMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table, RefBookStructure structure, String range) {
        VersionMapping versionMapping = super.getVersionMapping(refBookCode, refBookName, sourceCode, type, table, structure, range);
        String isDeletedField = "deleted_ts";
        if (structure.getAttributesAndTypes().containsKey(isDeletedField)) {
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        }
        versionMapping.setDeletedField(isDeletedField);
        return versionMapping;
    }
}
