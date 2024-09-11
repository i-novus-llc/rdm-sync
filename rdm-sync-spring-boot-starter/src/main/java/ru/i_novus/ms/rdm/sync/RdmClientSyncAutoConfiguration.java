package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDaoImpl;
import ru.i_novus.ms.rdm.sync.service.*;
import ru.i_novus.ms.rdm.sync.service.change_data.*;
import ru.i_novus.ms.rdm.sync.service.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.service.init.LocalRefBookCreatorLocator;
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
@EnableConfigurationProperties({RdmClientSyncProperties.class, RdmClientSyncLiquibaseParameters.class})
@AutoConfigureAfter(LiquibaseAutoConfiguration.class)
@EnableJms
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
    public RdmSyncDao rdmSyncDao() {
        return new RdmSyncDaoImpl();
    }

    @Bean(name = "publishDictionaryTopicMessageListenerContainerFactory")
    @ConditionalOnProperty(name = "rdm-sync.publish.listener.enable", havingValue = "true")
    @ConditionalOnClass(name = "org.apache.activemq.ActiveMQConnectionFactory")
    public DefaultJmsListenerContainerFactory unsharedPublishContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(false);

        return factory;
    }

    @Bean(name = "publishDictionaryTopicMessageListenerContainerFactory")
    @ConditionalOnProperty(name = "rdm-sync.publish.listener.enable", havingValue = "true")
    @ConditionalOnClass(name = "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory")
    public DefaultJmsListenerContainerFactory sharedPublishContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(true);

        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "rdm-sync.change_data.mode", havingValue = "async")
    public DefaultJmsListenerContainerFactory rdmChangeDataQueueMessageListenerContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);

        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "rdm-sync.publish.listener.enable", havingValue = "true")
    public PublishListener publishListener(RdmSyncService rdmSyncService) {
        return new PublishListener(rdmSyncService);
    }

    @Bean
    @ConditionalOnProperty(name = "rdm-sync.change_data.mode", havingValue = "async")
    public RdmChangeDataListener rdmChangeDataListener(@Value("${rdm.backend.path}") String url,
                                                       RdmChangeDataRequestCallback rdmChangeDataRequestCallback) {
        return new RdmChangeDataListener(url, rdmChangeDataRequestCallback);
    }

    @Bean
    @ConditionalOnProperty(value = "rdm-sync.change_data.mode", havingValue = "sync")
    public RdmChangeDataClient syncRdmChangeDataClient(@Value("${rdm.backend.path}") String url) {
        return new SyncRdmChangeDataClient(url);
    }

    @Bean
    @ConditionalOnProperty(value = "rdm-sync.change_data.mode", havingValue = "async")
    public RdmChangeDataClient asyncRdmChangeDataClient(JmsTemplate jmsTemplate,
                                                        @Value("${rdm-sync.change_data.queue:rdmChangeData}")
                                                        String rdmChangeDataQueue) {
        return new AsyncRdmChangeDataClient(jmsTemplate, rdmChangeDataQueue);
    }

    @Bean
    public RdmChangeDataRequestCallback rdmChangeDataRequestCallback() {
        return new RdmChangeDataRequestCallback.DefaultRdmChangeDataRequestCallback();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public RdmSyncLocalRowStateService rdmSyncLocalRowStateService() {
        return new RdmSyncLocalRowStateService(rdmSyncDao());
    }

    @Bean
    public LocalRdmDataService localRdmDataService() {
        return new LocalRdmDataServiceImpl();
    }


    @Bean
    public RefBookUpdater notVersionedRefBookUpdater(RdmSyncDao rdmSyncDao,
                                                     @Qualifier("notVersionedPersisterService") PersisterService persisterService,
                                                     RdmLoggingService rdmLoggingService,
                                                     VersionMappingService versionMappingService
                                                     ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }

    @Bean
    public RefBookUpdater rdmNotVersionedRefBookUpdater(RdmSyncDao rdmSyncDao,
                                                        @Qualifier("notVersionedPersisterService") PersisterService persisterService,
                                                        RdmLoggingService rdmLoggingService,
                                                        VersionMappingService versionMappingService
    ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }

    @Bean
    public RefBookUpdater simpleVersionedRefBookUpdater(RdmSyncDao rdmSyncDao,
                                                        @Qualifier("simpleVersionedPersisterService") PersisterService persisterService,
                                                        RdmLoggingService rdmLoggingService,
                                                        VersionMappingService versionMappingService
    ) {
        return new DefaultRefBookUpdater(rdmSyncDao, rdmLoggingService, persisterService, versionMappingService);
    }


    @Bean
    public RefBookUpdaterLocator refBookUpdaterLocator(@Qualifier("notVersionedRefBookUpdater") RefBookUpdater notVersionedRefBookUpdater,
                                                       @Qualifier("rdmNotVersionedRefBookUpdater") RefBookUpdater rdmNotVersionedRefBookUpdater,
                                                       @Qualifier("simpleVersionedRefBookUpdater") RefBookUpdater simpleVersionedRefBookUpdater) {
        return new RefBookUpdaterLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedRefBookUpdater,
                SyncTypeEnum.RDM_NOT_VERSIONED, rdmNotVersionedRefBookUpdater,
                SyncTypeEnum.SIMPLE_VERSIONED, simpleVersionedRefBookUpdater,
                SyncTypeEnum.NOT_VERSIONED_WITH_NATURAL_PK, notVersionedRefBookUpdater,
                SyncTypeEnum.RDM_NOT_VERSIONED_WITH_NATURAL_PK, rdmNotVersionedRefBookUpdater));
    }

    @Bean
    public LocalRefBookCreatorLocator localRefBookCreatorLocator(@Qualifier("notVersionedLocalRefBookCreator") LocalRefBookCreator notVersionedLocalRefBookCreator,
                                                                 @Qualifier("naturalPKLocalRefBookCreator") LocalRefBookCreator naturalPKLocalRefBookCreator,
                                                                 @Qualifier("simpleVersionedLocalRefBookCreator") LocalRefBookCreator simpleVersionedLocalRefBookCreator) {
        return new LocalRefBookCreatorLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedLocalRefBookCreator,
                SyncTypeEnum.SIMPLE_VERSIONED, simpleVersionedLocalRefBookCreator,
                SyncTypeEnum.RDM_NOT_VERSIONED, notVersionedLocalRefBookCreator,
                SyncTypeEnum.NOT_VERSIONED_WITH_NATURAL_PK, naturalPKLocalRefBookCreator,
                SyncTypeEnum.RDM_NOT_VERSIONED_WITH_NATURAL_PK, naturalPKLocalRefBookCreator));
    }
}
