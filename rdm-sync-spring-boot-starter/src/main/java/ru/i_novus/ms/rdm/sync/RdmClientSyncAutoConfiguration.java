package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.annotation.EnableJms;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataServiceV2;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDaoImpl;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDao;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDaoImpl;
import ru.i_novus.ms.rdm.sync.service.*;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;
import ru.i_novus.ms.rdm.sync.service.updater.DefaultRefBookUpdater;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdater;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdaterLocator;

import java.util.Map;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Configuration
@ConditionalOnClass(RdmSyncServiceImpl.class)
@ConditionalOnProperty(value = "rdm-sync.enabled", matchIfMissing = true)
@ComponentScan({"ru.i_novus.ms.rdm", "ru.i_novus.ms.fnsi"})
@EnableConfigurationProperties({RdmClientSyncProperties.class})
@AutoConfigureAfter(LiquibaseAutoConfiguration.class)
@EnableJms
@Slf4j
@SuppressWarnings("I-novus:MethodNameWordCountRule")
public class RdmClientSyncAutoConfiguration {

    @Autowired
    @Qualifier("cxfObjectMapper")
    private ObjectMapper objectMapper;

    @Bean
    @ConditionalOnMissingBean
    public RdmClientSyncConfig rdmClientSyncConfig(RdmClientSyncProperties properties) {

        String url = properties.getUrl();
        if (StringUtils.isEmpty(url))
            throw new IllegalArgumentException("Rdm client synchronizer properties not configured properly: url is missing");

        RdmClientSyncConfig config = new RdmClientSyncConfig();
        config.put("url", url);
        return config;
    }


    @Bean
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass(value = "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory")
    @ConditionalOnProperty(name = "rdm-sync.publish.listener.enable", havingValue = "true")
    public RdmSyncService lockingRdmSyncRest() {
        return new LockingRdmSyncService();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmSyncService rdmSyncRest() {
        return new RdmSyncServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMappingService rdmMappingService() {
        return new RdmMappingServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmLoggingService rdmLoggingService() {
        return new RdmLoggingService();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmSyncDao rdmSyncDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                 @Autowired(required = false) ru.i_novus.ms.rdm.sync.dao.TempTableCustomizer tempTableCustomizer) {
        return new RdmSyncDaoImpl(namedParameterJdbcTemplate, tempTableCustomizer);
    }

    @Bean
    @ConditionalOnProperty(name = "rdm-sync.publish.listener.enable", havingValue = "true")
    public PublishListener publishListener(RdmSyncService rdmSyncService) {
        return new PublishListener(rdmSyncService);
    }

    @Bean
    @ConditionalOnMissingBean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public LocalRdmDataService localRdmDataService() {
        return new LocalRdmDataServiceImpl();
    }

    @Bean
    public LocalRdmDataServiceV2 localRdmDataServiceV2() {
        return new LocalRdmDataServiceImplV2();
    }


    @Bean
    public RefBookUpdater notVersionedRefBookUpdater(
            RdmSyncDao rdmSyncDao,
            @Qualifier("notVersionedPersisterService") PersisterService persisterService,
            RdmLoggingService rdmLoggingService,
            VersionMappingService versionMappingService
    ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }

    @Bean
    public RefBookUpdater rdmNotVersionedRefBookUpdater(
            RdmSyncDao rdmSyncDao,
            @Qualifier("notVersionedPersisterService") PersisterService persisterService,
            RdmLoggingService rdmLoggingService,
            VersionMappingService versionMappingService
    ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }

    @Bean
    public RefBookUpdater simpleVersionedRefBookUpdater(
            RdmSyncDao rdmSyncDao,
            @Qualifier("simpleVersionedPersisterService") PersisterService persisterService,
            RdmLoggingService rdmLoggingService,
            VersionMappingService versionMappingService
    ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }

    @Bean
    public RefBookUpdater versionedRefBookUpdater(
            RdmSyncDao rdmSyncDao,
            @Qualifier("versionedPersisterService") PersisterService persisterService,
            RdmLoggingService rdmLoggingService,
            VersionMappingService versionMappingService
    ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }


    @Bean
    public RefBookUpdaterLocator refBookUpdaterLocator(
            @Qualifier("notVersionedRefBookUpdater") RefBookUpdater notVersionedRefBookUpdater,
            @Qualifier("rdmNotVersionedRefBookUpdater") RefBookUpdater rdmNotVersionedRefBookUpdater,
            @Qualifier("simpleVersionedRefBookUpdater") RefBookUpdater simpleVersionedRefBookUpdater,
            @Qualifier("versionedRefBookUpdater") RefBookUpdater versionedRefBookUpdater
    ) {
        return new RefBookUpdaterLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedRefBookUpdater,
                SyncTypeEnum.RDM_NOT_VERSIONED, rdmNotVersionedRefBookUpdater,
                SyncTypeEnum.SIMPLE_VERSIONED, simpleVersionedRefBookUpdater,
                SyncTypeEnum.VERSIONED, versionedRefBookUpdater,
                SyncTypeEnum.NOT_VERSIONED_WITH_NATURAL_PK, notVersionedRefBookUpdater,
                SyncTypeEnum.RDM_NOT_VERSIONED_WITH_NATURAL_PK, rdmNotVersionedRefBookUpdater));
    }



    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }

    @Bean
    @ConditionalOnMissingBean
    public VersionedDataDao versionedDataDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate, RdmSyncDao rdmSyncDao) {
        return new VersionedDataDaoImpl(namedParameterJdbcTemplate, rdmSyncDao);
    }
}
