package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Set;

@Component
public class NotVersionedWithNaturalPrimaryKeyLocalRefBookCreator extends NotVersionedLocalRefBookCreator{
    public NotVersionedWithNaturalPrimaryKeyLocalRefBookCreator(String schema, Boolean caseIgnore, RdmSyncDao dao, SyncSourceDao syncSourceDao, Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, caseIgnore, dao, syncSourceDao, syncSourceServiceFactories);
    }

    @Transactional
    @Override
    public void create(String refBookCode, String refBookName, String source, SyncTypeEnum type, String table, String sysPkColumn) {
        super.create(refBookCode, refBookName, source, type, table, sysPkColumn);
    }
}
