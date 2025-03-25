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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.*;

public class VersionedLocalRefBookCreatorTest extends DefaultLocalRefBookCreatorTest {

    static Set<RefBookStructure.Attribute> refAttributes = Set.of(
            new RefBookStructure.Attribute("ID", INTEGER, "Идентификатор"),
            new RefBookStructure.Attribute("NAME", STRING, "Наименование"),
            new RefBookStructure.Attribute("CREATE_DT", DATE, "Дата создания")
    );

    @TestConfiguration
    static class VersionedConfig {
        @Bean
        DefaultLocalRefBookCreator versionedCreator(
                @Qualifier("versionedLocalRefBookCreatorDao") LocalRefBookCreatorDao versionedDao,
                RefBookDescriptionService refBookDescriptionService,
                VersionMappingService versionMappingService) {

            return new DefaultLocalRefBookCreator(
                    "rdm",
                    false,
                    true,
                    versionedDao,
                    versionMappingService,
                    refBookDescriptionService
            );
        }
    }

    @Autowired
    private LocalRefBookCreator localRefBookCreator;

    /**
     * Первая загрузка маппинга и создание таблицы
     */
    @Test
    void testFirstCreate() {
        VersionMapping versionMapping = VersionMapping
                .builder()
                .code("TEST_VERSIONED")
                .table("rdm.ref_versioned")
                .source("FNSI")
                .sysPkColumn("_sync_rec_id")
                .primaryField("ID")
                .mappingVersion(-1)
                .refBookName("Версионный справочник")
                .type(SyncTypeEnum.VERSIONED)
                .build();
        List<FieldMapping> fieldMappings =  new ArrayList<>(List.of(
                new FieldMapping("id", "integer", "ID"),
                new FieldMapping("name", "varchar", "NAME"),
                new FieldMapping("create_dt", "date", "CREATE_DT")
        ));
        when(refBookDescriptionService.getRefBookDescription(any(SyncMapping.class)))
                .thenReturn(
                        new RefBookDescription(
                                "Тестовый справочник",
                                refAttributes.stream().collect(Collectors.toMap(attribute -> attribute.code().toLowerCase(), RefBookStructure.Attribute::description)
                                )
                        )
                );
        localRefBookCreator.create(
                new SyncMapping(versionMapping, fieldMappings));

        Assertions.assertTrue(tableExists("rdm", "ref_versioned"));
        Assertions.assertTrue(columnExists("rdm", "ref_versioned", "_sync_from_dt"));
        Assertions.assertTrue(columnExists("rdm", "ref_versioned", "_sync_to_dt"));
        Assertions.assertTrue(columnExists("rdm", "ref_versioned", "_sync_rec_id"));
        Assertions.assertTrue(columnExists("rdm", "ref_versioned", "_sync_hash"));

        refAttributes.forEach( attribute -> {
            Assertions.assertEquals(attribute.description(), getColumnComment("ref_versioned", attribute.code().toLowerCase()));
        });

        Assertions.assertEquals("Тестовый справочник", getTableComment("ref_versioned"));

    }
}
