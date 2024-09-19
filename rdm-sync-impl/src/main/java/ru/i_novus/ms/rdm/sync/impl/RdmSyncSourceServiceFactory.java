package ru.i_novus.ms.rdm.sync.impl;

import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

public class RdmSyncSourceServiceFactory implements SyncSourceServiceFactory {

    private final RestClient.Builder builder;

    public RdmSyncSourceServiceFactory(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public SyncSourceService createService(SyncSource source) {
        return new RdmSyncSourceService(builder.build());
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("RdmSyncSourceServiceFactory");
    }
}
