package ru.i_novus.ms.rdm.sync.impl;

import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

public class RdmSyncSourceServiceFactory implements SyncSourceServiceFactory {

    private final RefBookService refBookService;

    private final VersionRestService versionService;

    private final CompareService compareService;

    public RdmSyncSourceServiceFactory(RefBookService refBookService, VersionRestService versionService, CompareService compareService) {
        this.refBookService = refBookService;
        this.versionService = versionService;
        this.compareService = compareService;
    }

    @Override
    public SyncSourceService createService(SyncSource source) {
        return new RdmSyncSourceService(refBookService, versionService, compareService);
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("RdmSyncSourceServiceFactory");
    }
}
