package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NotVersionedLocalRefBookCreator implements LocalRefBookCreator {

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

    private final RdmSyncService rdmSyncService;

    private final String schema;

    public NotVersionedLocalRefBookCreator(RdmSyncDao dao, RdmSyncService rdmSyncService, @Value("${rdm_sync.auto_create.schema:rdm}") String schema) {
        this.dao = dao;
        this.rdmSyncService = rdmSyncService;
        this.schema = schema == null ? "rdm" : schema;
    }

    @Transactional
    @Override
    public void create(String refBookCode, String source) {
        if (dao.getVersionMapping(refBookCode, "CURRENT") != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
            return;
        }
        logger.info(LOG_AUTOCREATE_START, refBookCode);
        XmlMappingRefBook mapping = createMapping(refBookCode, source);
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;
        if(mapping != null) {
            createTable(refBookCode, mapping);
        }
    }

    private void createTable(String refBookCode, XmlMappingRefBook mapping) {
        String[] split = mapping.getSysTable().split("\\.");
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

    private XmlMappingRefBook createMapping(String refBookCode, String source) {
        RefBook lastPublished;
        try {
            lastPublished = rdmSyncService.getLastPublishedVersion(refBookCode);

        } catch (Exception e) {
            logger.error(LOG_AUTOCREATE_ERROR + LOG_LAST_PUBLISHED_NOT_FOUND, refBookCode, e);
            return null;
        }

        RefBookStructure structure = lastPublished.getStructure();
        String isDeletedField = "is_deleted";
        if (structure.getAttributesAndTypes().containsKey(isDeletedField)) {
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        }
        String uniqueSysField = structure.getPrimaries().get(0);

        XmlMappingRefBook mapping = new XmlMappingRefBook();
        mapping.setCode(refBookCode);
        mapping.setSysTable(String.format("%s.%s", schema, "ref_" + refBookCode.replaceAll("[-.]", "_").toLowerCase()));
        mapping.setDeletedField(isDeletedField);
        mapping.setUniqueSysField(uniqueSysField);
        mapping.setMappingVersion(-1);
        mapping.setSource(source);
        Integer mappingId = dao.insertVersionMapping(mapping);
        List<XmlMappingField> fields = new ArrayList<>(structure.getAttributesAndTypes().size() + 1);
        for (Map.Entry<String, AttributeTypeEnum> attr : structure.getAttributesAndTypes().entrySet()) {
            XmlMappingField field = new XmlMappingField();
            field.setRdmField(attr.getKey());
            field.setSysField(attr.getKey());
            field.setSysDataType(DataTypeEnum.getByRdmAttr(attr.getValue()).getDataTypes().get(0));
            fields.add(field);
        }

        dao.insertFieldMapping(mappingId, fields);
        return mapping;
    }
}
