package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(BaseLocalRefBookCreator.class);

    private static final String LOG_AUTOCREATE_ERROR =
            "Error autocreation mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_LAST_PUBLISHED_NOT_FOUND = " Can't get last published version from RDM.";

    private static final String LOG_AUTOCREATE_SKIP =
            "Skip autocreation of mapping data from structure of RefBook with code '{}'.";
    private static final String LOG_AUTOCREATE_START =
            "Autocreation mapping data from structure of RefBook with code '{}' is started.";



    protected final String schema;
    protected final boolean caseIgnore;

    protected final SyncSourceDao syncSourceDao;

    protected final RdmSyncDao dao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    protected abstract void createTable(String refBookCode, VersionMapping mapping);

    public BaseLocalRefBookCreator(String schema,
                                   Boolean caseIgnore,
                                   RdmSyncDao dao,
                                   SyncSourceDao syncSourceDao,
                                   Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        this.schema = schema == null ? "rdm" : schema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);

        this.syncSourceDao = syncSourceDao;
        this.dao = dao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
    }

    @Transactional
    @Override
    public void create(String refBookCode, String refBookName, String source, SyncTypeEnum type, String table) {

        if (dao.getVersionMapping(refBookCode, "CURRENT") != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
            return;
        }

        logger.info(LOG_AUTOCREATE_START, refBookCode);

        VersionMapping mapping = createMapping(refBookCode, refBookName, source, type, table);
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        if (mapping != null) {
            createTable(refBookCode, mapping);
        }
    }

    protected VersionMapping createMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table) {

        RefBookVersion lastPublished = getSyncSourceService(sourceCode).getRefBook(refBookCode, null);
        if (lastPublished == null) {
            throw new IllegalArgumentException(refBookCode + " not found in " + sourceCode);
        }

        RefBookStructure structure = lastPublished.getStructure();

        VersionMapping versionMapping = getVersionMapping(refBookCode, refBookName, sourceCode, type, table, structure);
        Integer mappingId = dao.insertVersionMapping(versionMapping);
        dao.insertFieldMapping(mappingId, getFieldMappings(structure));

        return versionMapping;
    }

    protected String getTableName(String refBookCode, String refBookTable) {

        String schemaName;
        String tableName;

        if (!StringUtils.isEmpty(refBookTable)) {

            String[] split = refBookTable.split("\\.");
            schemaName = (split.length > 1) ? split[0] : schema;
            tableName = (split.length > 1) ? split[1] : refBookTable;

        } else {
            schemaName = schema;
            tableName = refBookCode.replaceAll("[-.]", "_");
            tableName = "ref_" + (caseIgnore ? tableName.toLowerCase() : tableName);
        }
        
        return String.format("%s.%s", schemaName, tableName);
    }

    protected RefBookStructure getRefBookStructure(String refBookCode, String source) {
        RefBookVersion lastPublished;
        try {
            lastPublished = getSyncSourceService(source).getRefBook(refBookCode, null);

        } catch (Exception e) {
            logger.error(LOG_AUTOCREATE_ERROR + LOG_LAST_PUBLISHED_NOT_FOUND, refBookCode, e);
            return null;
        }

        return lastPublished.getStructure();
    }

    protected SyncSourceService getSyncSourceService(String sourceCode) {
        SyncSource source = syncSourceDao.findByCode(sourceCode);
        return syncSourceServiceFactories
                .stream()
                .filter(factory -> factory.isSatisfied(source))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("cannot find factory by " + source.getFactoryName()))
                .createService(source);
    }

    protected VersionMapping getVersionMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table, RefBookStructure structure) {
        String uniqueSysField =   caseIgnore ? structure.getPrimaries().get(0).toLowerCase() : structure.getPrimaries().get(0);

        String schemaTable = getTableName(refBookCode, table);

        return new VersionMapping(null, refBookCode, refBookName, null,
                schemaTable, sourceCode, uniqueSysField, null,
                null, -1, null, type);

    }

    protected List<FieldMapping> getFieldMappings(RefBookStructure structure) {
        List<FieldMapping> fieldMappings = new ArrayList<>(structure.getAttributesAndTypes().size() + 1);
        for (Map.Entry<String, AttributeTypeEnum> attr : structure.getAttributesAndTypes().entrySet()) {
            fieldMappings.add(new FieldMapping(
                    caseIgnore ? attr.getKey().toLowerCase() : attr.getKey(),
                    DataTypeEnum.getByRdmAttr(attr.getValue()).getDataTypes().get(0),
                    attr.getKey()
            ));
        }

        return fieldMappings;
    }

}
