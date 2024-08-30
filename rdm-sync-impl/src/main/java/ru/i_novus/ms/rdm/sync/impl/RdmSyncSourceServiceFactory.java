package ru.i_novus.ms.rdm.sync.impl;

import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

public class RdmSyncSourceServiceFactory implements SyncSourceServiceFactory {

    public RdmSyncSourceServiceFactory() {}

    @Override
    public SyncSourceService createService(SyncSource source) {
        return new RdmSyncSourceService();
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("RdmSyncSourceServiceFactory");
    }
}
