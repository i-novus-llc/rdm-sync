package ru.i_novus.ms.rdm.sync.service.updater;

import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

@Transactional
public class RdmNotVersionedRefBookUpdater extends BaseRefBookUpdater {

    private final PersisterService persisterService;

    public RdmNotVersionedRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, PersisterService persisterService, RdmLoggingService loggingService) {
        super(dao, syncSourceService, loggingService);
        this.persisterService = persisterService;
    }

    @Override
    protected PersisterService getPersisterService() {
        return persisterService;
    }
}
