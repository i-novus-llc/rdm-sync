package ru.i_novus.ms.fnsi.sync.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

@Configuration
public class FnsiConfig {

    @Bean
    public RestTemplate fnsiRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SyncSourceServiceFactory fnsiSyncSourceServiceFactory(
            @Qualifier("fnsiRestTemplate") RestTemplate fnsiRestTemplate
    ) {
        return new FnsiSyncSourceServiceFactory(fnsiRestTemplate());
    }

    @Bean
    public SourceLoaderService fnsiSourceLoaderService(
            FnsiSourceProperty property,
            @Qualifier("syncSourceDaoImpl") SyncSourceDao dao
    ) {
        return new FnsiSourceLoaderService(property, dao);
    }

}
