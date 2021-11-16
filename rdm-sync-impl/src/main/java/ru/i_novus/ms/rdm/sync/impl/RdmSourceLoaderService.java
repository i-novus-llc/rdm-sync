package ru.i_novus.ms.rdm.sync.impl;

import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

public class RdmSourceLoaderService implements SourceLoaderService {

    private final String url;

    private final SyncSourceDao dao;

    @Autowired
    public RdmSourceLoaderService(String url, SyncSourceDao dao) {
        this.url = url;
        this.dao = dao;
    }


    @Override
    public void load() {
        dao.save(new SyncSource("", "RDM", url, RdmSyncSourceService.class.getSimpleName()));
    }

}
