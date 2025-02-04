package ru.i_novus.ms.rdm.sync.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLocalRefBookCreator.class);


    private final String defaultSchema;

    protected final boolean caseIgnore;

    protected final LocalRefBookCreatorDao dao;

    protected final VersionMappingService versionMappingService;


    public DefaultLocalRefBookCreator(String defaultSchema,
                                         Boolean caseIgnore,
                                         LocalRefBookCreatorDao dao,
                                         VersionMappingService versionMappingService) {
        this.defaultSchema = defaultSchema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);
        this.dao = dao;
        this.versionMappingService = versionMappingService;
    }

    @Transactional
    @Override
    public void create(SyncMapping syncMapping, RefBookStructure structure) {
        String refBookCode = syncMapping.getVersionMapping().getCode();
        String range = syncMapping.getVersionMapping().getRange() != null ? syncMapping.getVersionMapping().getRange().getRange() : null;
        VersionMapping versionMapping = versionMappingService.getVersionMappingByCodeAndRange(refBookCode, range);
        saveMapping(syncMapping.getVersionMapping(), syncMapping.getFieldMapping(), versionMapping);
        String tableName = getTableNameWithSchema(refBookCode, syncMapping.getVersionMapping().getTable());

        if (!dao.tableExists(tableName)) {
            Map<String, String> attributeDescriptions = structure.getAttributes().stream()
                    .collect(Collectors.toMap(RefBookStructure.Attribute::code, RefBookStructure.Attribute::description));
            createTable(
                    tableName,
                    syncMapping.getVersionMapping(),
                    syncMapping.getFieldMapping(),
                    structure.getRefDescription(),
                    attributeDescriptions
            );
        } else {
            refreshTable(tableName, syncMapping.getFieldMapping());
        }
    }


    protected void createTable(String tableName,
                               VersionMapping mapping,
                               List<FieldMapping> fieldMappings,
                               String refDescription,
                               Map<String, String> fieldDescription) {

        dao.createTable(tableName, mapping.getCode(), mapping, fieldMappings, refDescription, fieldDescription);
        logger.info("Table {}  successfully prepared.", tableName);
    }



    protected void saveMapping(VersionMapping newVersionMapping, List<FieldMapping> fm, VersionMapping oldVersionMapping) {
        String refBookCode = newVersionMapping.getCode();
        String range = newVersionMapping.getRange() != null ? newVersionMapping.getRange().getRange() : null;

        if (oldVersionMapping == null) {
            Integer mappingId = dao.addMapping(newVersionMapping, fm);
            logger.info("mapping for code {} with range {} was added", refBookCode, range);
            newVersionMapping.setId(mappingId);
        } else if (newVersionMapping.getMappingVersion() > oldVersionMapping.getMappingVersion()) {
            logger.info("load {}", refBookCode);
            dao.updateMapping(oldVersionMapping.getMappingId(), newVersionMapping, fm);
            logger.info("mapping for code {} with range {} was updated", refBookCode, newVersionMapping.getMappingVersion());
        } else {
            logger.info("mapping for {} not changed", refBookCode);
        }
    }

    public String getTableNameWithSchema(String refBookCode, String refBookTable) {
        return RdmSyncInitUtils.buildTableNameWithSchema(refBookCode, refBookTable, defaultSchema, caseIgnore);
    }

    protected void refreshTable(String tableName, List<FieldMapping> fieldMappings) {
        List<String> columns = dao.getColumns(tableName);
        List<FieldMapping> newFieldMappings = fieldMappings.stream().filter(fieldMapping -> !columns.contains(fieldMapping.getSysField())).collect(Collectors.toList());
        if (!newFieldMappings.isEmpty()) {
            logger.info("change structure of table {}", tableName);
            dao.refreshTable(tableName, newFieldMappings, null, null);
        }
    }


}
