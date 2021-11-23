package ru.i_novus.ms.fnsi.sync.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

import java.util.Map;

public class FnsiSyncSourceServiceFactory implements SyncSourceServiceFactory {


    private final RestTemplate restTemplate;

    private final ObjectMapper mapper;

    public FnsiSyncSourceServiceFactory(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.mapper = new ObjectMapper();
    }


    @Override
    public SyncSourceService createService(SyncSource source) {

        Map<String, String> initValues = null;
        try {
            initValues = mapper.readValue(source.getInitValues(), Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
        String userKey = initValues.get("userKey");
        String fnsiUrl = initValues.get("url");

        return new FnsiSyncSourceService(restTemplate, fnsiUrl, userKey);
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("FnsiSyncSourceServiceFactory");
    }
}
