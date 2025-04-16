package ru.i_novus.ms.rdm.sync.init.init;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescriptionService;
import ru.i_novus.ms.rdm.sync.init.liquibase.RdmClientSyncLiquibaseParameters;

import javax.sql.DataSource;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@Testcontainers
@Import({DefaultLocalRefBookCreatorTest.Config.class})
@Sql(scripts = {"/test-source.sql", "/doc_type.sql"})
abstract class DefaultLocalRefBookCreatorTest {

    @MockBean
    RefBookDescriptionService refBookDescriptionService;

    @TestConfiguration
    @ComponentScan(value = {
            "ru.i_novus.ms.rdm.sync.init.dao",
            "ru.i_novus.ms.rdm.sync.init.liquibase"
    })
    @EnableConfigurationProperties({RdmClientSyncLiquibaseParameters.class})
    static class Config {

        @MockBean
        VersionMappingService versionMappingService;


        @Bean
        public DataSource dataSource() {
            return new org.springframework.jdbc.datasource.DriverManagerDataSource(
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

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("rdm_sync")
            .withUsername("postgresql")
            .withPassword("postgresql");


    @Autowired
    private LocalRefBookCreator versionedCreator;

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    protected String getColumnComment(String tableName, String columnName) {
        // SQL-запрос для получения комментария к колонке
        String sql = """
            SELECT d.description
            FROM pg_description d
            JOIN pg_class c ON c.oid = d.objoid
            JOIN pg_attribute a ON a.attnum = d.objsubid AND a.attrelid = c.oid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE c.relname = :tableName
              AND a.attname = :columnName
              AND n.nspname = 'rdm'
            """;

        // Параметры для запроса
        Map<String, Object> params = Map.of(
                "tableName", tableName,
                "columnName", columnName
        );

        // Выполнение запроса и возврат результата
        return namedParameterJdbcTemplate.queryForList(sql, params, String.class)
                .stream()
                .findFirst().orElse(null);
    }

    protected String getTableComment(String tableName) {
        // SQL-запрос для получения комментария к таблице
        String sql = """
                SELECT obj_description(c.oid)
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE c.relname = :tableName
                  AND n.nspname = 'rdm';
                """;

        // Параметры для запроса
        Map<String, Object> params = Map.of("tableName", tableName);

        return namedParameterJdbcTemplate.queryForObject(sql, params, String.class);
    }

    protected boolean tableExists(String schemaName, String tableName) {
        String sql = "SELECT EXISTS ("
                + "SELECT 1 FROM pg_catalog.pg_tables "
                + "WHERE schemaname = :schema AND tablename = :table"
                + ")";

        return Boolean.TRUE.equals(
                namedParameterJdbcTemplate.queryForObject(
                        sql,
                        Map.of("schema", schemaName, "table", tableName),
                        Boolean.class
                )
        );
    }
    protected boolean columnExists(String schemaName, String tableName, String columnName) {
        String sql = "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = :schema AND table_name = :table AND column_name = :column"
                + ")";

        return Boolean.TRUE.equals(
                namedParameterJdbcTemplate.queryForObject(
                        sql,
                        Map.of("schema", schemaName, "table", tableName, "column", columnName),
                        Boolean.class
                )
        );
    }
}
