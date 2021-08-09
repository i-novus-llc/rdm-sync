package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.rest.RdmSyncService;
import ru.i_novus.ms.rdm.sync.service.RdmSyncDao;

import java.util.ArrayList;
import java.util.List;

@Component
class LocalTableAutoCreateService {

    private static final Logger logger = LoggerFactory.getLogger(LocalTableAutoCreateService.class);

    private static final String LOG_AUTOCREATE_SKIP =
            "Skip autocreation of mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_AUTOCREATE_START =
            "Autocreation mapping data from structure of RefBook with code '{}' is started.";
    private static final String LOG_AUTOCREATE_FINISH =
            "Autocreation mapping data from structure of RefBook with code '{}' is finished.";
    private static final String LOG_AUTOCREATE_ERROR =
            "Error autocreation mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_LAST_PUBLISHED_NOT_FOUND = " Can't get last published version from RDM.";

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private RdmSyncService rdmSyncService;

    @Transactional
    public void autoCreate(String refBookCode, String autoCreateSchema) {

        if (dao.getVersionMapping(refBookCode) != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
            return;
        }

        logger.info(LOG_AUTOCREATE_START, refBookCode);

        RefBook lastPublished;
        try {
            lastPublished = rdmSyncService.getLastPublishedVersionFromRdm(refBookCode);

        } catch (Exception e) {
            logger.error(LOG_AUTOCREATE_ERROR + LOG_LAST_PUBLISHED_NOT_FOUND, refBookCode, e);
            return;
        }

        Structure structure = lastPublished.getStructure();
        String isDeletedField = "is_deleted";
        if (structure.getAttribute(isDeletedField) != null) {
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        }

        Structure.Attribute uniqueSysField = structure.getPrimaries().get(0);

        XmlMappingRefBook mapping = new XmlMappingRefBook();
        mapping.setCode(refBookCode);
        mapping.setSysTable(String.format("%s.%s", autoCreateSchema, refBookCode.replaceAll("[-.]", "_").toLowerCase()));
        mapping.setDeletedField(isDeletedField);
        mapping.setUniqueSysField(uniqueSysField.getCode());
        mapping.setMappingVersion(-1);

        List<XmlMappingField> fields = new ArrayList<>(structure.getAttributes().size() + 1);
        for (Structure.Attribute attr : structure.getAttributes()) {
            XmlMappingField field = new XmlMappingField();
            field.setRdmField(attr.getCode());
            field.setSysField(attr.getCode());
            field.setSysDataType(DataTypeEnum.getByRdmAttr(attr).getDataTypes().get(0));
            fields.add(field);
        }

        dao.upsertVersionMapping(mapping);
        dao.insertFieldMapping(refBookCode, fields);

        logger.info(LOG_AUTOCREATE_FINISH, refBookCode);
    }
}
