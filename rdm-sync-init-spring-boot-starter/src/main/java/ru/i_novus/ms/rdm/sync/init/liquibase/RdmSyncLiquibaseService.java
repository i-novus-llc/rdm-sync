package ru.i_novus.ms.rdm.sync.init.liquibase;

import jakarta.annotation.PostConstruct;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Запускает liquibase для миграции структуры необходимой для работы синхронизации
 * Обернут в отдельный класс чтобы не отключался дефолтный бин liquibase
 * Это миграция выполняется до запуска дефолтного liquibase, который предоставляет spring boot
 */
@Service
@Order(1)
@Slf4j
public class RdmSyncLiquibaseService {
    private final DataSource dataSource;
    private final RdmClientSyncLiquibaseParameters parameters;
    private final ApplicationContext applicationContext;

    public RdmSyncLiquibaseService(DataSource dataSource, RdmClientSyncLiquibaseParameters parameters, ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.parameters = parameters;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() throws LiquibaseException {
        final SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(applicationContext);
        liquibase.setDatabaseChangeLogLockTable("databasechangeloglock_rdms");
        if(!parameters.isQuartzEnabled()) {
            log.info("disabled quartz schemas initialization");
            liquibase.setChangeLog("classpath*:/rdm-sync-db/baseChangelog.xml");

        } else {
            log.info("enabled quartz schemas initialization");
            liquibase.setChangeLog("classpath*:/rdm-sync-db/baseChangelogWithQuartz.xml");

            final Map<String, String> changeLogParameters = new HashMap<>(2);
            changeLogParameters.put("quartz_schema_name", parameters.getQuartzSchemaName());
            changeLogParameters.put("quartz_table_prefix", parameters.getQuartzTablePrefix());
            liquibase.setChangeLogParameters(changeLogParameters);
        }

        liquibase.afterPropertiesSet();
    }
}
