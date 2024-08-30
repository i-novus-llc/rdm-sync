package ru.i_novus.ms.rdm.sync.impl;

import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

@ConditionalOnProperty(name = "rdm.backend.path", havingValue = "")
@EnableJaxRsProxyClient(
        address = "${rdm.client.sync.url}"
)
@Configuration
public class RdmSyncImplConfig {

    @Bean
    public SyncSourceServiceFactory rdmSyncSourceServiceFactory() {
        return new RdmSyncSourceServiceFactory();
    }

    @Bean
    public SourceLoaderService rdmSourceLoaderService(
            @Value("${rdm.backend.path}") String url, @Qualifier("syncSourceDaoImpl") SyncSourceDao dao) {
        return new RdmSourceLoaderService(url, dao);
    }
}
