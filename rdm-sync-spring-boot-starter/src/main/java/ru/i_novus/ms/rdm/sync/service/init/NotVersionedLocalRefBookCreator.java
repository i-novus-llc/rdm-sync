package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String LOG_AUTOCREATE_SKIP =
            "Skip autocreation of mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_AUTOCREATE_START =
            "Autocreation mapping data from structure of RefBook with code '{}' is started.";
    private static final String LOG_AUTOCREATE_FINISH =
            "Autocreation mapping data from structure of RefBook with code '{}' is finished.";
    private static final String LOG_AUTOCREATE_ERROR =
            "Error autocreation mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_LAST_PUBLISHED_NOT_FOUND = " Can't get last published version from RDM.";

    private static final Logger logger = LoggerFactory.getLogger(NotVersionedLocalRefBookCreator.class);

    private final RdmSyncDao dao;


    public NotVersionedLocalRefBookCreator(@Value("${rdm-sync.auto_create.schema:rdm}") String schema,
                                           RdmSyncDao dao,
                                           SyncSourceDao syncSourceDao,
                                           Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, syncSourceDao, syncSourceServiceFactories);

        this.dao = dao;
    }

    @Transactional
    @Override
    public void create(String refBookCode, String refBookName, String source, SyncTypeEnum type) {

        if (dao.getVersionMapping(refBookCode, "CURRENT") != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
            return;
        }

        logger.info(LOG_AUTOCREATE_START, refBookCode);

        VersionMapping mapping = createMapping(refBookCode, refBookName, source, type);
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        if(mapping != null) {
            createTable(refBookCode, mapping);
        }
    }

    private void createTable(String refBookCode, VersionMapping mapping) {

        String[] split = mapping.getTable().split("\\.");
        String schema = split[0];
        String table = split[1];

        dao.createSchemaIfNotExists(schema);
        dao.createTableIfNotExists(schema, table, dao.getFieldMappings(refBookCode), mapping.getDeletedField());

        logger.info("Preparing table {} in schema {}.", table, schema);

        dao.addInternalLocalRowStateColumnIfNotExists(schema, table);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, table);

        logger.info("Table {} in schema {} successfully prepared.", table, schema);
    }

    private VersionMapping createMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type) {

        RefBook lastPublished = getSyncSourceService(sourceCode).getRefBook(refBookCode);
        if (lastPublished == null) {
            throw new IllegalArgumentException(refBookCode + " not found in " + sourceCode);
        }

        RefBookStructure structure = lastPublished.getStructure();
        String isDeletedField = "deleted_ts";
        if (structure.getAttributesAndTypes().containsKey(isDeletedField)) {
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        }
        String uniqueSysField = structure.getPrimaries().get(0);

        String sysTable = getTableName(refBookCode);

        VersionMapping versionMapping = new VersionMapping(null, refBookCode, refBookName, null, sysTable, sourceCode, uniqueSysField, isDeletedField, null, -1, null, SyncTypeEnum.NOT_VERSIONED);
        Integer mappingId = dao.insertVersionMapping(versionMapping);

        List<FieldMapping> fields = new ArrayList<>(structure.getAttributesAndTypes().size() + 1);
        for (Map.Entry<String, AttributeTypeEnum> attr : structure.getAttributesAndTypes().entrySet()) {
            fields.add(new FieldMapping(attr.getKey(), DataTypeEnum.getByRdmAttr(attr.getValue()).getDataTypes().get(0), attr.getKey()));
        }
        dao.insertFieldMapping(mappingId, fields);

        return versionMapping;
    }
}
