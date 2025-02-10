package ru.i_novus.ms.rdm.sync.init;

import jakarta.annotation.PostConstruct;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescriptionService;
import ru.i_novus.ms.rdm.sync.init.liquibase.RdmClientSyncLiquibaseParameters;
import ru.i_novus.ms.rdm.sync.init.liquibase.RdmSyncLiquibaseService;

import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties({RdmClientSyncLiquibaseParameters.class})
public class RdmSyncInitAutoconfiguration {

    @PostConstruct
    public void init() {
        log.info("RdmSyncInitAutoconfiguration loaded!");
    }



    @Bean
    public LocalRefBookCreatorLocator localRefBookCreatorLocator(@Value("${rdm-sync.auto-create.schema:rdm}") String schema,
                                                                 @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
                                                                 @Value("${rdm-sync.auto-create.refresh-comments:true}") Boolean refreshComments,
                                                                 @Qualifier("notVersionedLocalRefBookCreatorDao") LocalRefBookCreatorDao notVersionedDao,
                                                                 @Qualifier("naturalPKLocalRefBookCreatorDao") LocalRefBookCreatorDao naturalPKDao,
                                                                 @Qualifier("simpleVersionedLocalRefBookCreatorDao") LocalRefBookCreatorDao versionedDao,
                                                                 VersionMappingService versionMappingService,
                                                                 RefBookDescriptionService refBookDescriptionService) {
        LocalRefBookCreator notVersionedLocalRefBookCreator = new DefaultLocalRefBookCreator(
                schema,
                caseIgnore,
                refreshComments,
                notVersionedDao,
                versionMappingService,
                refBookDescriptionService
        );
        LocalRefBookCreator naturalPKLocalRefBookCreator = new DefaultLocalRefBookCreator(
                schema,
                caseIgnore,
                refreshComments,
                naturalPKDao,
                versionMappingService,
                refBookDescriptionService
        );
        LocalRefBookCreator simpleVersionedLocalRefBookCreator = new DefaultLocalRefBookCreator(
                schema,
                caseIgnore,
                refreshComments,
                versionedDao,
                versionMappingService,
                refBookDescriptionService
        );
        return new LocalRefBookCreatorLocator(Map.of(
                SyncTypeEnum.NOT_VERSIONED, notVersionedLocalRefBookCreator,
                SyncTypeEnum.SIMPLE_VERSIONED, simpleVersionedLocalRefBookCreator,
                SyncTypeEnum.RDM_NOT_VERSIONED, notVersionedLocalRefBookCreator,
                SyncTypeEnum.NOT_VERSIONED_WITH_NATURAL_PK, naturalPKLocalRefBookCreator,
                SyncTypeEnum.RDM_NOT_VERSIONED_WITH_NATURAL_PK, naturalPKLocalRefBookCreator));
    }

    @Bean
    public SpringLiquibaseDependsOnPostProcessor springLiquibaseDependsOnPostProcessor() {
        return new SpringLiquibaseDependsOnPostProcessor();
    }

    /**
     * Like DependOn but programmatically
     */
    public static class SpringLiquibaseDependsOnPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

        SpringLiquibaseDependsOnPostProcessor() {
            // Configure the 3rd party SpringLiquibase bean to depend on our RdmSyncLiquibaseService
            super(SpringLiquibase.class, RdmSyncLiquibaseService.class);
        }
    }

}
