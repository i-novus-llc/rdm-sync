package ru.i_novus.ms.rdm.sync.service.mapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.loader.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.model.loader.AutoCreateRefBookPropertyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static ru.i_novus.ms.rdm.sync.service.init.RdmSyncInitUtils.buildTableNameWithSchema;

@Component
public class PropMappingSourceService implements MappingSourceService {

    private static final int DEFAULT_VERSION_FOR_PROPERTY_MAPPING = -2;

    private final SyncSourceDao syncSourceDao;

    protected final RdmSyncDao dao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    private final boolean caseIgnore;

    private final String defaultSchema;

    private AutoCreateRefBookProperty autoCreateRefBookProperties;

    @Autowired
    public PropMappingSourceService(SyncSourceDao syncSourceDao, RdmSyncDao dao,
                                    Set<SyncSourceServiceFactory> syncSourceServiceFactories,
                                    @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                    @Value("${rdm-sync.auto-create.schema:rdm}") String defaultSchema,
                                    AutoCreateRefBookProperty autoCreateRefBookProperties) {
        this.syncSourceDao = syncSourceDao;
        this.dao = dao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
        this.caseIgnore = caseIgnore;
        this.defaultSchema = defaultSchema;
        this.autoCreateRefBookProperties = autoCreateRefBookProperties;
    }

    @Override
    public List<VersionAndFieldMapping> getVersionAndFieldMappingList() {

        List<AutoCreateRefBookPropertyValue> autoCreateOnPropValues =
                autoCreateRefBookProperties == null || autoCreateRefBookProperties.getRefbooks() == null
                        ? new ArrayList<>()
                        : autoCreateRefBookProperties.getRefbooks();
        List<VersionAndFieldMapping> versionAndFieldMappings = new ArrayList<>();

        autoCreateOnPropValues.forEach(refbook ->
                versionAndFieldMappings.add(createMapping(refbook)));

        return versionAndFieldMappings;
    }

    private VersionAndFieldMapping createMapping(AutoCreateRefBookPropertyValue refbook) {
        RefBookVersion lastPublished = getSyncSourceService(refbook.getSource()).getRefBook(refbook.getCode(), null);
        if (lastPublished == null) {
            throw new IllegalArgumentException(refbook.getCode() + " not found in " + refbook.getCode());
        }
        return new VersionAndFieldMapping(
                DEFAULT_VERSION_FOR_PROPERTY_MAPPING,
                generateVersionMapping(refbook,  lastPublished.getStructure()),
                generateFieldMappings(lastPublished.getStructure())
        );
    }

    private VersionMapping generateVersionMapping(AutoCreateRefBookPropertyValue refbook, RefBookStructure structure) {
        String schemaTable = buildTableNameWithSchema(refbook.getCode(), refbook.getTable(), defaultSchema, TRUE.equals(caseIgnore));
        String uniqueSysField = caseIgnore ? structure.getPrimaries().get(0).toLowerCase() : structure.getPrimaries().get(0);

        String isDeletedField = "deleted_ts";
        if (structure.getAttributesAndTypes().containsKey(isDeletedField)) {
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        }

        return new VersionMapping(null, refbook.getCode(), refbook.getName(), null, schemaTable,
                refbook.getSysPkColumn(), refbook.getSource(), uniqueSysField, isDeletedField, null,
                DEFAULT_VERSION_FOR_PROPERTY_MAPPING, null, refbook.getType(), refbook.getRange());
    }



    private List<FieldMapping> generateFieldMappings(RefBookStructure structure) {
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

    private SyncSourceService getSyncSourceService(String sourceCode) {
        SyncSource source = syncSourceDao.findByCode(sourceCode);
        return syncSourceServiceFactories
                .stream()
                .filter(factory -> factory.isSatisfied(source))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("cannot find factory by " + source.getFactoryName()))
                .createService(source);
    }


}
