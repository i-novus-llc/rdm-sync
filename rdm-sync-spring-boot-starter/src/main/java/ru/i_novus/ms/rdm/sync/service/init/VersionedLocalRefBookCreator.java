package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

/**
 * пока не используется, предполагался использоваться в более оптимальном хранении версионности
 */
@Component
public class VersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private static final Logger logger = LoggerFactory.getLogger(VersionedLocalRefBookCreator.class);

    private final RdmSyncDao rdmSyncDao;


    public VersionedLocalRefBookCreator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                        @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                        RdmSyncDao rdmSyncDao,
                                        SyncSourceDao syncSourceDao,
                                        VersionMappingService versionMappingService) {
        super(schema, caseIgnore, rdmSyncDao, syncSourceDao, versionMappingService);

        this.rdmSyncDao = rdmSyncDao;
    }


    @Override
    public void create(SyncMapping syncMapping) {
        String refBookCode =  syncMapping.getVersionMapping().getCode();
        String refBookName = syncMapping.getVersionMapping().getRefBookName();
        String source = syncMapping.getVersionMapping().getSource();
        SyncTypeEnum type = syncMapping.getVersionMapping().getType();
        String table = syncMapping.getVersionMapping().getTable();
        String sysPkColumn = syncMapping.getVersionMapping().getSysPkColumn();
        Range range = syncMapping.getVersionMapping().getRange();
        String pk = syncMapping.getFieldMapping().get(0).getSysField();
        if(rdmSyncDao.existsLoadedVersion(refBookCode)) {
            logger.info("auto create for code {} was skipped", refBookCode);
            return;
        }

        logger.info("starting auto create for code {}", refBookCode);
        VersionMapping versionMapping = versionMappingService.getVersionMapping(refBookCode, null);
        if (versionMapping == null) {

            String schemaTable = getTableNameWithSchema(refBookCode, table);
            versionMapping = new VersionMapping(null, refBookCode, refBookName, null,
                    schemaTable, sysPkColumn,source, pk, null,
                    null, -1, null, type,range, true, syncMapping.getVersionMapping().isRefreshableRange());
            versionMapping.setId(rdmSyncDao.insertVersionMapping(versionMapping));
        }

        createTable(refBookCode, versionMapping);

        logger.info("auto create for code {} was finished", refBookCode);
    }

    protected void createTable(String code, VersionMapping versionMapping) {
        String[] split = getTableNameWithSchema(code, versionMapping.getTable()).split("\\.");
        String schemaName = split[0];
        String tableName = split[1];

        rdmSyncDao.createSchemaIfNotExists(schemaName);
        rdmSyncDao.createVersionedTableIfNotExists(schemaName, tableName,
                rdmSyncDao.getFieldMappings(versionMapping.getId()), versionMapping.getSysPkColumn());
    }
}
