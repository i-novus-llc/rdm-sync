package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
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

import javax.annotation.Nullable;
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



    private final String defaultSchema;
    private final boolean caseIgnore;

    protected final SyncSourceDao syncSourceDao;

    protected final RdmSyncDao dao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    protected abstract void createTable(String refBookCode, VersionMapping mapping);

    public BaseLocalRefBookCreator(String defaultSchema,
                                   Boolean caseIgnore,
                                   RdmSyncDao dao,
                                   SyncSourceDao syncSourceDao,
                                   Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        this.defaultSchema = defaultSchema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);

        this.syncSourceDao = syncSourceDao;
        this.dao = dao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
    }

    @Transactional
    @Override
    public void create(String refBookCode, String refBookName, String source, SyncTypeEnum type, String table, @Nullable String range) {

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode, "CURRENT");
        if (versionMapping != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
        } else {
            logger.info(LOG_AUTOCREATE_START, refBookCode);
            versionMapping = createMapping(refBookCode, refBookName, source, type, table, range);
        }
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        if (versionMapping != null) {
            createTable(refBookCode, versionMapping);
        }
    }

    protected VersionMapping createMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table, @Nullable String range) {
                                                                        //todo нужна ли версия
        RefBookVersion lastPublished = getSyncSourceService(sourceCode).getRefBook(refBookCode, null);
        if (lastPublished == null) {
            throw new IllegalArgumentException(refBookCode + " not found in " + sourceCode);
        }

        RefBookStructure structure = lastPublished.getStructure();

        VersionMapping versionMapping = getVersionMapping(refBookCode, refBookName, sourceCode, type, table, structure, range);
        Integer mappingId = dao.insertVersionMapping(versionMapping);
        dao.insertFieldMapping(mappingId, getFieldMappings(structure));

        versionMapping.setId(mappingId);
        return versionMapping;
    }

    public String getTableNameWithSchema(String refBookCode, String refBookTable) {
        return RdmSyncInitUtils.buildTableNameWithSchema(refBookCode, refBookTable, defaultSchema, caseIgnore);
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

    protected VersionMapping getVersionMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table, RefBookStructure structure, @Nullable String range) {
        String uniqueSysField =   caseIgnore ? structure.getPrimaries().get(0).toLowerCase() : structure.getPrimaries().get(0);

        String schemaTable = getTableNameWithSchema(refBookCode, table);

        return new VersionMapping(null, refBookCode, refBookName, null,
                schemaTable, sourceCode, uniqueSysField, null,
                null, -1, null, type, range);

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
