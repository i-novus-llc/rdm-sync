package ru.i_novus.ms.rdm.sync.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

@ConditionalOnProperty(name = "rdm.backend.path", havingValue = "")
@Configuration
public class RdmSyncImplConfig {

    @Bean
    public RestClient.Builder rdmRestClientBuilder(
            @Value("${rdm.backend.path}") String url
    ) {
        return RestClient.builder().baseUrl(url);
    }

    @Bean
    public SyncSourceServiceFactory rdmSyncSourceServiceFactory(
            @Qualifier("rdmRestClientBuilder") RestClient.Builder rdmRestClientBuilder
    ) {
        return new RdmSyncSourceServiceFactory(rdmRestClientBuilder);
    }

    @Bean
    public SourceLoaderService rdmSourceLoaderService(
            @Value("${rdm.backend.path}") String url,
            @Qualifier("syncSourceDaoImpl") SyncSourceDao dao
    ) {
        return new RdmSourceLoaderService(url, dao);
    }
}
