package ru.i_novus.ms.rdm.sync.init.init;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.DefaultLocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.LocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescription;
import ru.i_novus.ms.rdm.sync.init.description.RefBookDescriptionService;
import ru.i_novus.ms.rdm.sync.init.mapping.utils.MappingCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.INTEGER;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.STRING;

public class NotVersionedLocalRefBookCreatorTest  extends DefaultLocalRefBookCreatorTest {

    static Set<RefBookStructure.Attribute> ek003Attributes = Set.of(
            new RefBookStructure.Attribute("id", INTEGER, "Идентификатор"),
            new RefBookStructure.Attribute("name", STRING, "Наименование")
    );

    @TestConfiguration
    static class NotVersionedConfig {
        @Bean
        DefaultLocalRefBookCreator notVersionedCreator(
                @Qualifier("notVersionedLocalRefBookCreatorDao") LocalRefBookCreatorDao notVersionedDao,
                RefBookDescriptionService refBookDescriptionService,
                VersionMappingService versionMappingService) {

            return new DefaultLocalRefBookCreator(
                    "rdm",
                    false,
                    true,
                    notVersionedDao,
                    versionMappingService,
                    refBookDescriptionService
            );
        }
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
        when(refBookDescriptionService.getRefBookDescription(any(SyncMapping.class)))
                .thenReturn(
                        new RefBookDescription(
                                "Тестовый справочник",
                                ek003Attributes.stream().collect(Collectors.toMap(RefBookStructure.Attribute::code, RefBookStructure.Attribute::description)
                                )
                        )
                );
        notVersionedCreator.create(
                new SyncMapping(versionMapping, fieldMappings));

        ek003Attributes.forEach( attribute -> {
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
        Map<String, String> attributeDescr1 = ek003Attributes.stream().collect(Collectors.toMap(RefBookStructure.Attribute::code, RefBookStructure.Attribute::description));
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
}
