package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.integration.spring.SpringLiquibase;
import net.n2oapp.platform.jaxrs.LocalDateTimeISOParameterConverter;
import net.n2oapp.platform.jaxrs.TypedParamConverter;
import net.n2oapp.platform.jaxrs.autoconfigure.MissingGenericBean;
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
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.provider.*;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDaoImpl;
import ru.i_novus.ms.rdm.sync.service.*;
import ru.i_novus.ms.rdm.sync.service.change_data.*;
import ru.i_novus.ms.rdm.sync.service.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.service.init.LocalRefBookCreatorLocator;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;
import ru.i_novus.ms.rdm.sync.service.updater.*;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
    @DependsOn("liquibase")
    public SpringLiquibase liquibaseRdm(DataSource dataSource, RdmClientSyncLiquibaseParameters parameters) {

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDatabaseChangeLogLockTable("databasechangeloglock_rdms");
        liquibase.setChangeLog("classpath*:/rdm-sync-db/baseChangelog.xml");

        Map<String, String> changeLogParameters = new HashMap<>(2);
        changeLogParameters.put("quartz_schema_name", parameters.getQuartzSchemaName());
        changeLogParameters.put("quartz_table_prefix", parameters.getQuartzTablePrefix());
        liquibase.setChangeLogParameters(changeLogParameters);

        return liquibase;
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

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<LocalDateTime> mskUtcLocalDateTimeParamConverter() {
        return new MskUtcLocalDateTimeParamConverter(new LocalDateTimeISOParameterConverter());
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<LocalDate> isoLocaldateParamConverter() {
        return new IsoLocalDateParamConverter();
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<AttributeFilter> attributeFilterConverter() {
        return new AttributeFilterConverter(objectMapper);
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<OffsetDateTime> offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }


    @Bean
    @ConditionalOnMissingBean
    public ExportFileProvider exportFileProvider() {
        return new ExportFileProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMapperConfigurer rdmMapperConfigurer() {
        return new RdmMapperConfigurer();
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
    public RdmChangeDataListener rdmChangeDataListener(RefBookService refBookService,
                                                       RdmChangeDataRequestCallback rdmChangeDataRequestCallback) {
        return new RdmChangeDataListener(refBookService, rdmChangeDataRequestCallback);
    }

    @Bean
    @ConditionalOnProperty(value = "rdm-sync.change_data.mode", havingValue = "sync")
    public RdmChangeDataClient syncRdmChangeDataClient(RefBookService refBookService) {
        return new SyncRdmChangeDataClient(refBookService);
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
                                                     SyncSourceService syncSourceService,
                                                     @Qualifier("notVersionedPersisterService") PersisterService persisterService,
                                                     RdmLoggingService rdmLoggingService
                                                     ) {
        return new NotVersionedRefBookUpdater(rdmSyncDao, syncSourceService, persisterService, rdmLoggingService);
    }

    @Bean
    public RefBookUpdater rdmNotVersionedRefBookUpdater(RdmSyncDao rdmSyncDao,
                                                        SyncSourceService syncSourceService,
                                                        @Qualifier("notVersionedPersisterService") PersisterService persisterService,
                                                        RdmLoggingService rdmLoggingService
    ) {
        return new RdmNotVersionedRefBookUpdater(rdmSyncDao, syncSourceService, persisterService, rdmLoggingService);
    }

    @Bean
    public RefBookUpdater simpleVersionedRefBookUpdater(RdmSyncDao rdmSyncDao,
                                                        SyncSourceService syncSourceService,
                                                        @Qualifier("simpleVersionedPersisterService") PersisterService persisterService,
                                                        RdmLoggingService rdmLoggingService
    ) {
        return new SimpleVersionedRefBookUpdater(rdmSyncDao, syncSourceService, persisterService, rdmLoggingService);
    }


    @Bean
    public RefBookUpdaterLocator refBookUpdaterLocator(@Qualifier("notVersionedRefBookUpdater") RefBookUpdater notVersionedRefBookUpdater,
                                                       @Qualifier("rdmNotVersionedRefBookUpdater") RefBookUpdater rdmNotVersionedRefBookUpdater,
                                                       @Qualifier("simpleVersionedRefBookUpdater") RefBookUpdater simpleVersionedRefBookUpdater) {
        return new RefBookUpdaterLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedRefBookUpdater,
                SyncTypeEnum.RDM_NOT_VERSIONED, rdmNotVersionedRefBookUpdater,
                SyncTypeEnum.SIMPLE_VERSIONED, simpleVersionedRefBookUpdater));
    }

    @Bean
    public LocalRefBookCreatorLocator localRefBookCreatorLocator(@Qualifier("notVersionedLocalRefBookCreator") LocalRefBookCreator notVersionedLocalRefBookCreator,
                                                                 @Qualifier("simpleVersionedLocalRefBookCreator") LocalRefBookCreator simpleVersionedLocalRefBookCreator) {
        return new LocalRefBookCreatorLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedLocalRefBookCreator,
                SyncTypeEnum.SIMPLE_VERSIONED, simpleVersionedLocalRefBookCreator,
                SyncTypeEnum.RDM_NOT_VERSIONED, notVersionedLocalRefBookCreator));
    }
}
