package ru.i_novus.ms.fnsi.sync.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

@Configuration
public class FnsiConfig {

    @ConditionalOnProperty(value = "rdm_sync.fnsi.url", havingValue = "")
    @Bean
    public FnsiSyncSourceService fnsiSyncSourceService(@Value("${rdm_sync.fnsi.url:#{null}}") String fnsiUrl, @Value("${rdm_sync.fnsi.userKey}") String userKey, @Autowired RestTemplate restTemplate) {
        return new FnsiSyncSourceService(restTemplate, fnsiUrl, userKey);
    }

}
