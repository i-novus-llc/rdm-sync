package ru.i_novus.ms.rdm.sync.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescription;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescriptionService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultLocalRefBookCreator implements LocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLocalRefBookCreator.class);


    private final String defaultSchema;

    protected final boolean caseIgnore;

    protected final LocalRefBookCreatorDao dao;

    protected final VersionMappingService versionMappingService;

    protected final RefBookDescriptionService refBookDescriptionService;

    private final boolean refreshComments;


    public DefaultLocalRefBookCreator(String defaultSchema,
                                      Boolean caseIgnore,
                                      Boolean refreshComments,
                                      LocalRefBookCreatorDao dao,
                                      VersionMappingService versionMappingService,
                                      RefBookDescriptionService refBookDescriptionService) {
        this.defaultSchema = defaultSchema;
        this.caseIgnore = Boolean.TRUE.equals(caseIgnore);
        this.refreshComments = Boolean.TRUE.equals(refreshComments);
        this.dao = dao;
        this.versionMappingService = versionMappingService;
        this.refBookDescriptionService = refBookDescriptionService;
    }

    @Transactional
    @Override
    public void create(SyncMapping syncMapping) {
        String refBookCode = syncMapping.getVersionMapping().getCode();
        RefBookDescription refBookDescription = refBookDescriptionService.getRefBookDescription(syncMapping);
        String range = syncMapping.getVersionMapping().getRange() != null ? syncMapping.getVersionMapping().getRange().getRange() : null;
        VersionMapping versionMapping = versionMappingService.getVersionMappingByCodeAndRange(refBookCode, range);
        saveMapping(syncMapping.getVersionMapping(), syncMapping.getFieldMapping(), versionMapping);
        String tableName = getTableNameWithSchema(refBookCode, syncMapping.getVersionMapping().getTable());

        if (!dao.tableExists(tableName)) {

            createTable(
                    tableName,
                    syncMapping.getVersionMapping(),
                    syncMapping.getFieldMapping(),
                    refBookDescription.refDescription(),
                    refBookDescription.attributeDescriptions()
            );
        } else {
            refreshTable(
                    tableName,
                    syncMapping.getFieldMapping(),
                    refBookDescription.refDescription(),
                    refBookDescription.attributeDescriptions()
            );
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

    protected void refreshTable(String tableName,
                                List<FieldMapping> fieldMappings,
                                String refDescription,
                                Map<String, String> fieldDescription) {

        List<String> columns = dao.getColumns(tableName);
        List<FieldMapping> newFieldMappings = fieldMappings.stream().filter(fieldMapping -> !columns.contains(fieldMapping.getSysField())).collect(Collectors.toList());
        if (!newFieldMappings.isEmpty()) {
            logger.info("change structure of table {}", tableName);
            dao.refreshTable(tableName, newFieldMappings, refDescription, fieldDescription);
        }
        if (refreshComments) {
            logger.info("try to refresh comments for {}", tableName);
            dao.addCommentsIfNotExists(tableName, refDescription, fieldMappings, fieldDescription);
        }
    }


}
