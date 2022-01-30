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

@Component
public class NaturalPKLocalRefBookCreator extends NotVersionedLocalRefBookCreator {

    Logger logger = LoggerFactory.getLogger(NaturalPKLocalRefBookCreator.class);

    public NaturalPKLocalRefBookCreator(
            @Value("${rdm-sync.auto-create.schema:rdm}") String schema,
            @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
            RdmSyncDao dao, SyncSourceDao syncSourceDao,
            Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, caseIgnore, dao, syncSourceDao, syncSourceServiceFactories);
    }

    @Override
    protected void createTable(String refBookCode, VersionMapping mapping) {

        String[] split = mapping.getTable().split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        dao.createSchemaIfNotExists(schemaName);
        dao.createTableWithNaturalPrimaryKeyIfNotExists(schemaName, tableName, dao.getFieldMappings(refBookCode), mapping.getDeletedField(), mapping.getSysPkColumn());

        logger.info("Preparing table {} in schema {}.", tableName, schemaName);

        dao.addInternalLocalRowStateColumnIfNotExists(schema, tableName);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, tableName);

        logger.info("Table {} in schema {} successfully prepared.", tableName, schemaName);
    }

    @Override
    protected VersionMapping getVersionMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table, RefBookStructure structure, String sysPkColumn, String range) {
        String sysPkColumnFromUniqueSysField = caseIgnore ? structure.getPrimaries().get(0).toLowerCase() : structure.getPrimaries().get(0);
        return super.getVersionMapping(refBookCode, refBookName, sourceCode, type, table, structure, sysPkColumnFromUniqueSysField, range);
    }
}
