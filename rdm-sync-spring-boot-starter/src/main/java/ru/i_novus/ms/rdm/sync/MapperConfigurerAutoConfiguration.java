package ru.i_novus.ms.rdm.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.provider.RdmMapperConfigurer;
import ru.i_novus.ms.rdm.sync.service.RdmSyncServiceImpl;

@Configuration
@ConditionalOnClass(RdmSyncServiceImpl.class)
@ConditionalOnProperty(value = "rdm-sync.enabled", matchIfMissing = true)
public class MapperConfigurerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RdmMapperConfigurer rdmMapperConfigurer() {
        return new RdmMapperConfigurer();
    }
}
