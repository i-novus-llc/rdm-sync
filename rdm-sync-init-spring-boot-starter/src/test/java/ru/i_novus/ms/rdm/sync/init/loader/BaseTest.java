package ru.i_novus.ms.rdm.sync.init.loader;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.init.liquibase.RdmClientSyncLiquibaseParameters;

import javax.sql.DataSource;

@ExtendWith(SpringExtension.class)
@Testcontainers
@Import({BaseTest.Config.class})
abstract class BaseTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("rdm_sync")
            .withUsername("postgresql")
            .withPassword("postgresql");

    @TestConfiguration
    @ComponentScan(value = {
            "ru.i_novus.ms.rdm.sync.init.dao",
            "ru.i_novus.ms.rdm.sync.init.liquibase"
    })
    @EnableConfigurationProperties({RdmClientSyncLiquibaseParameters.class})
    static class Config {

        @Bean
        public DataSource dataSource() {
            return new DriverManagerDataSource(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()
            );
        }

        @Bean
        public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
            return new NamedParameterJdbcTemplate(dataSource);
        }
    }

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;


}
