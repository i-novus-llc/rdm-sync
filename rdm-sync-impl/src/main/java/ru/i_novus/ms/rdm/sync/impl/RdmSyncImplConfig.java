package ru.i_novus.ms.rdm.sync.impl;

import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;

@ConditionalOnProperty(name = "rdm.backend.path", havingValue = "")
@EnableJaxRsProxyClient(
        classes = {RefBookService.class, VersionRestService.class, CompareService.class},
        address = "${rdm.client.sync.url}"
)
@Configuration
public class RdmSyncImplConfig {

    @Bean
    public RdmSyncSourceService rdmSyncSourceService(RefBookService refBookService, VersionRestService versionService, CompareService compareService) {
        return new RdmSyncSourceService(refBookService, versionService, compareService);
    }
}
