package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(BaseLocalRefBookCreator.class);


    private final String defaultSchema;

    protected final boolean caseIgnore;

    protected final SyncSourceDao syncSourceDao;

    protected final RdmSyncDao dao;

    protected final VersionMappingService versionMappingService;

    protected abstract void createTable(String schemaName, String tableName, VersionMapping mapping, List<FieldMapping> fieldMappings);

    protected BaseLocalRefBookCreator(String defaultSchema,
                                      Boolean caseIgnore,
                                      RdmSyncDao dao,
                                      SyncSourceDao syncSourceDao, VersionMappingService versionMappingService) {
        this.defaultSchema = defaultSchema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);
        this.syncSourceDao = syncSourceDao;
        this.dao = dao;
        this.versionMappingService = versionMappingService;
    }

    @Transactional
    @Override
    public void create(SyncMapping syncMapping) {
        String refBookCode = syncMapping.getVersionMapping().getCode();
        String range = syncMapping.getVersionMapping().getRange() != null ? syncMapping.getVersionMapping().getRange().getRange() : null;
        VersionMapping versionMapping = versionMappingService.getVersionMappingByCodeAndRange(refBookCode, range);
        saveMapping(syncMapping.getVersionMapping(), syncMapping.getFieldMapping(), versionMapping);
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        String[] split = getTableNameWithSchema(refBookCode, syncMapping.getVersionMapping().getTable()).split("\\.");
        String schemaName = split[0];
        String tableName = split[1];
        if (!dao.tableExists(schemaName, tableName)) {
            createTable(schemaName, tableName, syncMapping.getVersionMapping(), syncMapping.getFieldMapping());
        } else {
            refreshTable(schemaName, tableName, syncMapping.getFieldMapping());
        }
    }

    protected void saveMapping(VersionMapping newVersionMapping, List<FieldMapping> fm, VersionMapping oldVersionMapping) {
        String refBookCode = newVersionMapping.getCode();
        String range = newVersionMapping.getRange() != null ? newVersionMapping.getRange().getRange() : null;

        if (oldVersionMapping == null) {
            Integer mappingId = dao.insertVersionMapping(newVersionMapping);
            dao.insertFieldMapping(mappingId, fm);
            logger.info("mapping for code {} with range {} was added", refBookCode, range);
            newVersionMapping.setId(mappingId);
        } else if (newVersionMapping.getMappingVersion() > oldVersionMapping.getMappingVersion()) {
            logger.info("load {}", refBookCode);
            dao.updateCurrentMapping(newVersionMapping);
            dao.insertFieldMapping(oldVersionMapping.getMappingId(), fm);
            logger.info("mapping for code {} with range {} was updated", refBookCode, newVersionMapping.getMappingVersion());
        } else {
            logger.info("mapping for {} not changed", refBookCode);
        }
    }

    public String getTableNameWithSchema(String refBookCode, String refBookTable) {
        return RdmSyncInitUtils.buildTableNameWithSchema(refBookCode, refBookTable, defaultSchema, caseIgnore);
    }

    protected void refreshTable(String schemaName, String tableName, List<FieldMapping> fieldMappings) {
        List<String> columns = dao.getColumns(schemaName, tableName);
        List<FieldMapping> newFieldMappings = fieldMappings.stream().filter(fieldMapping -> !columns.contains(fieldMapping.getSysField())).collect(Collectors.toList());
        dao.refreshTable(schemaName, tableName, newFieldMappings);
    }
}
