package ru.i_novus.ms.rdm.sync.service.mapping;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import java.util.Collections;
import java.util.List;

/**
 * Тест кейсы для лоадера источника маппинга из *.xml
 */
@ExtendWith(MockitoExtension.class)
class XmlMappingSourceServiceTest {

    @InjectMocks
    private XmlMappingSourceService xmlMappingSourceService;

    @BeforeEach
    public void setUp() throws Exception {
        xmlMappingSourceService.setRdmMappingXmlPath("/mapping-sources/rdm-mapping.xml");
    }

    /**
     * Получение списка {@link VersionMapping} из *.xml
     */
    @Test
    void testGetVersionMappingListFromXmlFromXml() {
        VersionMapping expectedVersionMapping = new VersionMapping(
                null,
                "EK003",
                "Справочник",
                null,
                "rdm.ref_ek003",
                "_sync_rec_id",
                "RDM",
                "id",
                "deleted_ts",
                null,
                1,
                null,
                SyncTypeEnum.NOT_VERSIONED,
                null,
                false,
                false);
        List<FieldMapping> expectedFieldMappings = List.of(
                new FieldMapping("ref", "varchar", "ref"),
                new FieldMapping("name_ru", "varchar", "name_ru"),
                new FieldMapping("code_en", "varchar", "code_en"),
                new FieldMapping("id", "integer", "id"),
                new FieldMapping("is_cold", "boolean", "is_cold"),
                new FieldMapping("some_ignored_field", "varchar", "some_ignored_field", true),
                new FieldMapping("def_val_field", "varchar", "def_val_field", "some default value")
        );
        SyncMapping expected = new SyncMapping(expectedVersionMapping, expectedFieldMappings);

        SyncMapping mapping = xmlMappingSourceService.getMappings().get(0);

        Assertions.assertEquals(1, xmlMappingSourceService.getMappings().size());
        Assertions.assertEquals(expected, mapping);
    }

    /**
     * Ситуация когда лоадер источника маппинга не нашел *.xml файл, в этом случае должен вернуться пустой список
     */
    @Test
    void testLoaderSourceMappingNotFoundXml() {
        xmlMappingSourceService.setRdmMappingXmlPath("/another-rdm-mapping.xml");
        List<SyncMapping> expectedEmptyVersionMappingList = Collections.emptyList();
        List<SyncMapping> actualEmptyVersionMappingList = xmlMappingSourceService.getMappings();

        Assertions.assertEquals(expectedEmptyVersionMappingList, actualEmptyVersionMappingList);
    }


    /**
     * Ситуация когда путь к *.xml задан некорректно
     */
    @Test
    void testXmlMappingLoadError() {
        xmlMappingSourceService.setRdmMappingXmlPath("");
        Assertions.assertThrows(RdmException.class, () -> xmlMappingSourceService.getMappings());
    }

}
