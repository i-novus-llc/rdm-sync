package ru.i_novus.ms.rdm.sync;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.provider.*;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.json.LocalDateTimeMapperPreparer;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDaoImpl;
import ru.i_novus.ms.rdm.sync.quartz.AutowiringSpringBeanJobFactory;
import ru.i_novus.ms.rdm.sync.service.*;
import ru.i_novus.ms.rdm.sync.service.change_data.*;
import ru.i_novus.ms.rdm.sync.service.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.service.init.LocalRefBookCreatorLocator;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdater;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdaterLocator;

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
@EnableConfigurationProperties({RdmClientSyncProperties.class})
@AutoConfigureAfter(LiquibaseAutoConfiguration.class)
@EnableJms
@ComponentScan({"ru.i_novus.ms.rdm", "ru.i_novus.ms.fnsi"})
@ConditionalOnProperty(
        value = "rdm_sync.enabled",
        matchIfMissing = true)
public class RdmClientSyncAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Bean
    public AutowiringSpringBeanJobFactory autowiringSpringBeanJobFactory() {
        return new AutowiringSpringBeanJobFactory(applicationContext);
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {

        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(autowiringSpringBeanJobFactory());
        schedulerFactory.setConfigLocation(new ClassPathResource("quartz.properties"));
        schedulerFactory.setDataSource(dataSource);

        return schedulerFactory;
    }

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
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
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
        return new AttributeFilterConverter();
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<OffsetDateTime> offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new LocalDateTimeMapperPreparer();
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
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    @ConditionalOnClass(name = "org.apache.activemq.ActiveMQConnectionFactory")
    public DefaultJmsListenerContainerFactory unsharedPublishContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(false);

        return factory;
    }

    @Bean(name = "publishDictionaryTopicMessageListenerContainerFactory")
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    @ConditionalOnClass(name = "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory")
    public DefaultJmsListenerContainerFactory sharedPublishContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(true);

        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data.mode", havingValue = "async")
    public DefaultJmsListenerContainerFactory rdmChangeDataQueueMessageListenerContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);

        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    public PublishListener publishListener(RdmSyncService rdmSyncService) {
        return new PublishListener(rdmSyncService);
    }

    @Bean
    @ConditionalOnProperty(name = "rdm_sync.change_data.mode", havingValue = "async")
    public RdmChangeDataListener rdmChangeDataListener(RefBookService refBookService,
                                                       RdmChangeDataRequestCallback rdmChangeDataRequestCallback) {
        return new RdmChangeDataListener(refBookService, rdmChangeDataRequestCallback);
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data.mode", havingValue = "sync")
    public RdmChangeDataClient syncRdmChangeDataClient(RefBookService refBookService) {
        return new SyncRdmChangeDataClient(refBookService);
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data.mode", havingValue = "async")
    public RdmChangeDataClient asyncRdmChangeDataClient(JmsTemplate jmsTemplate,
                                                        @Value("${rdm_sync.change_data.queue:rdmChangeData}")
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
        return new RdmSyncLocalRowStateService();
    }

    @Bean
    public LocalRdmDataService localRdmDataService() {
        return new LocalRdmDataServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RefBookUpdaterLocator refBookUpdaterLocator(@Qualifier("notVersionedRefBookUpdater") RefBookUpdater notVersionedRefBookUpdater) {
        return new RefBookUpdaterLocator(Map.of(SyncTypeEnum.NOT_VERSIONED, notVersionedRefBookUpdater));
    }

    @Bean
    public LocalRefBookCreatorLocator localRefBookCreatorLocator(@Qualifier("notVersionedLocalRefBookCreator") LocalRefBookCreator notVersionedLocalRefBookCreator,
                                                                 @Qualifier("versionedLocalRefBookCreator") LocalRefBookCreator versionedLocalRefBookCreator) {
        return new LocalRefBookCreatorLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedLocalRefBookCreator,
                SyncTypeEnum.VERSIONED, versionedLocalRefBookCreator));
    }
}
