package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Set;

@Component
public class VersionedLocalRefBookCreator extends BaseLocalRefBookCreator {

    private final RdmSyncDao rdmSyncDao;


    public VersionedLocalRefBookCreator(@Value("${rdm_sync.auto_create.schema:rdm}") String schema, RdmSyncDao rdmSyncDao, SyncSourceDao syncSourceDao, Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, syncSourceDao, syncSourceServiceFactories);
        this.rdmSyncDao = rdmSyncDao;
    }


    @Override
    public void create(String code, String source) {
        VersionMapping versionMapping = rdmSyncDao.getVersionMapping(code, "CURRENT");
        if(versionMapping == null) {
            RefBookStructure refBookStructure = getRefBookStructure(code, source);
            //todo создать маппинг и под версию
            versionMapping = new VersionMapping(null, code, "CURRENT",  getTableName(code),"someSource", refBookStructure.getPrimaries().get(0), null, null, -1, null, SyncTypeEnum.VERSIONED);
            rdmSyncDao.insertVersionMapping(versionMapping);
        }

        String[] split = versionMapping.getTable().split("\\.");
        String schema = split[0];
        String table = split[1];
        rdmSyncDao.createSchemaIfNotExists(schema);
        rdmSyncDao.createVersionedTableIfNotExists(schema, table, rdmSyncDao.getFieldMappings(code));
    }
}
