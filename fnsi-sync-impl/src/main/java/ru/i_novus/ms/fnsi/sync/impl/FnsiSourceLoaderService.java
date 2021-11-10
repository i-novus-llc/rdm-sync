package ru.i_novus.ms.fnsi.sync.impl;

import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

public class FnsiSourceLoaderService implements SourceLoaderService {

    private FnsiSourceProperty property;

    private final SyncSourceDao dao;

    public FnsiSourceLoaderService(FnsiSourceProperty property, SyncSourceDao dao) {
        this.dao = dao;
        this.property = property;
    }

    @Override
    public void load() {
        property.getValues().forEach(fnsiSourcePropertyValue ->
                dao.save(new SyncSource(
                fnsiSourcePropertyValue.getName(),
                fnsiSourcePropertyValue.getCode(),
                String.format("{\"userKey\":\"%s\", \"url\":\"%s\"}",
                        fnsiSourcePropertyValue.getUserKey(), fnsiSourcePropertyValue.getUrl())
        )));
    }
}
