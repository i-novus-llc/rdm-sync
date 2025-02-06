package ru.i_novus.ms.rdm.sync.init.init;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure.Attribute;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.DefaultLocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescription;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescriptionService;
import ru.i_novus.ms.rdm.sync.init.liquibase.RdmClientSyncLiquibaseParameters;
import ru.i_novus.ms.rdm.sync.init.mapping.utils.MappingCreator;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.INTEGER;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.STRING;

@ExtendWith(SpringExtension.class)
@Testcontainers
@Import({DefaultLocalRefBookCreatorTest.Config.class})
@Sql(scripts = {"/test-source.sql", "/doc_type.sql"})
class DefaultLocalRefBookCreatorTest {

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

        static Set<Attribute> ek003Attributes = Set.of(
                new Attribute("id", INTEGER, "Идентификатор"),
                new Attribute("name", STRING, "Наименование")
        );

        @Bean
        DefaultLocalRefBookCreator notVersionedCreator(
                @Qualifier("notVersionedLocalRefBookCreatorDao") LocalRefBookCreatorDao notVersionedDao,
                RefBookDescriptionService refBookDescriptionService) {

            return new DefaultLocalRefBookCreator(
                    "rdm",
                    false,
                    notVersionedDao,
                    versionMappingService,
                    refBookDescriptionService
            );
        }

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
    private LocalRefBookCreator notVersionedCreator;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;



    /**
     * Первая загрузка маппинга и создание таблицы
     */
    @Test
    void testFirstCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        when(refBookDescriptionService.getRefBookDescription(any(SyncMapping.class)))
                .thenReturn(
                        new RefBookDescription(
                                "Тестовый справочник",
                                Config.ek003Attributes.stream().collect(Collectors.toMap(Attribute::code, Attribute::description)
                                )
                        )
                );
        notVersionedCreator.create(
                new SyncMapping(versionMapping, fieldMappings));

        Config.ek003Attributes.forEach( attribute -> {
            Assertions.assertEquals(attribute.description(), getColumnComment("ref_ek003", attribute.code()));
        });

        Assertions.assertEquals("Тестовый справочник", getTableComment("ref_ek003"));

    }

    @Test
    void testRefresh() {
        String refCode = "testRefBook";
        String tableName = "rdm.ref_test";
        VersionMapping versionMapping = VersionMapping.builder()
                .code(refCode)
                .refBookName("Справочник")
                .table(tableName)
                .sysPkColumn("_sync_rec_id")
                .source("RDM")
                .primaryField("id")
                .deletedField("deleted_ts")
                .mappingVersion(-1)
                .type(SyncTypeEnum.NOT_VERSIONED)
                .build();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        Map<String, String> attributeDescr1 = Config.ek003Attributes.stream().collect(Collectors.toMap(Attribute::code, Attribute::description));
        Map<String, String> attributeDescr2 = new HashMap<>(attributeDescr1);
        attributeDescr2.put("new_field", "Новый атрибут");
        when(refBookDescriptionService.getRefBookDescription(any(SyncMapping.class)))
                .thenReturn(
                        new RefBookDescription("Тестовый справочник", attributeDescr1),
                        new RefBookDescription("Тестовый справочник", attributeDescr2)
                );
        notVersionedCreator.create(new SyncMapping(versionMapping, fieldMappings));

        // редактируем
        List<FieldMapping> newFieldMappings = MappingCreator.createFieldMapping();
        newFieldMappings.add(new FieldMapping("new_field", "varchar", "new_field"));
        notVersionedCreator.create(new SyncMapping(versionMapping, newFieldMappings));

        Assertions.assertEquals("Тестовый справочник", getTableComment("ref_test"));
        attributeDescr2.forEach((key, value) -> Assertions.assertEquals(value, getColumnComment("ref_test", key)));

    }

    @Test
    void testAddCommentsIfNotExists() {
        String code = "1.2.643.5.1.13.13.99.2.48";
        String table = "document_type";
        VersionMapping versionMapping = VersionMapping.builder()
                   .code(code)
                   .refBookName(table)
                   .source("FNSI")
                   .table("rdm." + table)
                   .sysPkColumn("code")
                   .primaryField("code")
                   .deletedField("deleted_ts")
                   .type(SyncTypeEnum.NOT_VERSIONED_WITH_NATURAL_PK)
                   .build();

        List<FieldMapping> fieldMappings = List.of(
                new FieldMapping("code", "integer", "ID"),
                new FieldMapping("parent_id", "integer", "PARENT_ID"),
                new FieldMapping("name", "varchar", "NAME"),
                new FieldMapping("actual", "boolean", "ACTUAL")
        );

        when(refBookDescriptionService.getRefBookDescription(any(SyncMapping.class)))
                .thenReturn(
                        new RefBookDescription(
                                "Тип документа",
                                Map.of("code", "Идентификатор", "parent_id", "Род. запись", "name", "Наименование", "actual", "Флаг")
                        )
                );

        notVersionedCreator.create(new SyncMapping(versionMapping, fieldMappings));

        Assertions.assertEquals("Тип документа", getTableComment(table));
        Assertions.assertEquals("Идентификатор", getColumnComment(table, "code"));
        Assertions.assertEquals("Род. запись", getColumnComment(table, "parent_id"));
        Assertions.assertEquals("Наименование", getColumnComment(table, "name"));
        Assertions.assertEquals("Флаг", getColumnComment(table, "actual"));

    }

    private String getColumnComment(String tableName, String columnName) {
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

    public String getTableComment(String tableName) {
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
}
