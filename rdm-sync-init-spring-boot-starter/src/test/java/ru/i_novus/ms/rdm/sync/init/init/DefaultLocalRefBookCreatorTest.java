package ru.i_novus.ms.rdm.sync.init.init;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.DefaultLocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.init.mapping.utils.MappingCreator;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.*;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.DATE;

@ExtendWith(SpringExtension.class)
@Testcontainers
@Import(DefaultLocalRefBookCreatorTest.Config.class)
class DefaultLocalRefBookCreatorTest {

    @TestConfiguration
    @ComponentScan("ru.i_novus.ms.rdm.sync.init.dao")
    static class Config {

        @MockBean
        VersionMappingService versionMappingService;

        @Bean
        DefaultLocalRefBookCreator notVersionedCreator(@Qualifier("notVersionedLocalRefBookCreatorDao") LocalRefBookCreatorDao notVersionedDao) {
            return new DefaultLocalRefBookCreator("rdm", false, notVersionedDao, versionMappingService);
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

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        // Динамически настраиваем свойства Spring Boot для использования Testcontainers
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private LocalRefBookCreator notVersionedCreator;



    /**
     * Первая загрузка маппинга и создание таблицы
     */
    @Test
    void testFirstCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        RefBookStructure refBookStructure = new RefBookStructure(
                emptyList(),
                List.of("ID"),
                Set.of(
                        new RefBookStructure.Attribute("ID", INTEGER, null),
                        new RefBookStructure.Attribute("NAME", STRING, null),
                        new RefBookStructure.Attribute("CODE", STRING, null),
                        new RefBookStructure.Attribute("RAZDEL", INTEGER, null),
                        new RefBookStructure.Attribute("DATE_BEGIN", DATE, null),
                        new RefBookStructure.Attribute("DATE_END", DATE, null)
                )
        );
        notVersionedCreator.create(
                new SyncMapping(versionMapping, fieldMappings), refBookStructure);
//        creator.create(new SyncMapping(versionMapping, fieldMappings));
//        verify(dao).addMapping(versionMapping, fieldMappings);
    }

    /**
     * Маппинг не менялся и таблицы уже созданы
     */
   /* @Test
    void testNoCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        when(versionMappingService.getVersionMappingByCodeAndRange(any(), any())).thenReturn(versionMapping);
        when(dao.tableExists(anyString())).thenReturn(true);
        creator.create(new SyncMapping( versionMapping, fieldMappings));
        verify(dao, never()).addMapping(any(VersionMapping.class), anyList());
    }*/

    /**
     * Маппинг изменился, а таблица уже есть
     */
    /*@Test
    void testMappingChanged() {
        VersionMapping newVersionMapping = MappingCreator.createVersionMapping();
        newVersionMapping.setMappingVersion(1);
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        VersionMapping oldVersionMapping = MappingCreator.createVersionMapping();
        oldVersionMapping.setMappingId(55);
        when(versionMappingService.getVersionMappingByCodeAndRange(eq(oldVersionMapping.getCode()), any())).thenReturn(oldVersionMapping);
        when(dao.tableExists(anyString())).thenReturn(true);
        creator.create(new SyncMapping(newVersionMapping, fieldMappings));
        verify(dao).updateMapping(oldVersionMapping.getMappingId(), newVersionMapping, fieldMappings);
    }*/
}
