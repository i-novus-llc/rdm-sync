package ru.i_novus.ms.rdm.sync.admin.n2o;

import net.n2oapp.cache.template.SyncCacheTemplate;
import net.n2oapp.framework.api.register.MetadataRegister;
import net.n2oapp.framework.config.compile.pipeline.operation.CompileCacheOperation;
import net.n2oapp.framework.config.compile.pipeline.operation.SourceCacheOperation;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.sync.admin.n2o.operation.RdmSyncCompileCacheOperation;
import ru.i_novus.ms.rdm.sync.admin.n2o.operation.RdmSyncSourceCacheOperation;

@Configuration
@SuppressWarnings({"rawtypes","java:S3740"})
public class SyncN2oConfig {

    @Bean
    public CompileCacheOperation compileCacheOperation(CacheManager cacheManager) {
        return new RdmSyncCompileCacheOperation(new SyncCacheTemplate(cacheManager));
    }

    @Bean
    public SourceCacheOperation sourceCacheOperation(CacheManager cacheManager, MetadataRegister metadataRegister) {
        return new RdmSyncSourceCacheOperation(new SyncCacheTemplate(cacheManager), metadataRegister);
    }
}
