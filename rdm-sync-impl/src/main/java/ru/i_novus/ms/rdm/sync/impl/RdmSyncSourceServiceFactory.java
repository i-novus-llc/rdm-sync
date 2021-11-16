package ru.i_novus.ms.rdm.sync.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

public class RdmSyncSourceServiceFactory implements SyncSourceServiceFactory {

    private final RestTemplate restTemplate;

    private final ObjectMapper mapper;

    public RdmSyncSourceServiceFactory(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper();
    }

    @Override
    public SyncSourceService createService(SyncSource source) {
        //TODO
        return null;
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("RdmSyncSourceServiceFactory");
    }
}
