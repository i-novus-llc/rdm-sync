package ru.i_novus.ms.rdm.sync.service.updater;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

@Transactional
public class SimpleVersionedRefBookUpdater extends BaseRefBookUpdater {

    private final PersisterService persisterService;

    public SimpleVersionedRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, PersisterService persisterService, RdmLoggingService loggingService) {
        super(dao, syncSourceService, loggingService);
        this.persisterService = persisterService;
    }

    @Override
    protected PersisterService getPersisterService() {
        return persisterService;
    }
}
