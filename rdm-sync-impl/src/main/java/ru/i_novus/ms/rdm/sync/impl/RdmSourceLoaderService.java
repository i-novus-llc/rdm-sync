package ru.i_novus.ms.rdm.sync.impl;

import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

public class RdmSourceLoaderService implements SourceLoaderService {

    private final String url;

    private final SyncSourceDao dao;


    public RdmSourceLoaderService (String url, SyncSourceDao dao){
        this.url = url;
        this.dao = dao;
    }

    @Override
    public void load() {
        dao.save(new SyncSource("","", url));
    }
}
