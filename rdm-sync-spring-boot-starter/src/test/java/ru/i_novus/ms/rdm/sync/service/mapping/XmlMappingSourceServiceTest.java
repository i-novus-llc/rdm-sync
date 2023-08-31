package ru.i_novus.ms.rdm.sync.service.mapping;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import java.util.Collections;
import java.util.List;

/**
 * Тест кейсы для лоадера источника маппинга из *.xml
 */
@ExtendWith(MockitoExtension.class)
public class XmlMappingSourceServiceTest {

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
    public void testGetVersionMappingListFromXmlFromXml() {
        VersionMapping expectedVersionMapping = MappingCreator.createVersionMapping();
        VersionAndFieldMapping actualVersionMapping = xmlMappingSourceService.getVersionAndFieldMappingList().get(0);

        Assertions.assertEquals(expectedVersionMapping.getCode(), actualVersionMapping.getVersionMapping().getCode());
    }

    /**
     * Ситуация когда лоадер источника маппинга не нашел *.xml файл, в этом случае должен вернуться пустой список
     */
    @Test
    public void testLoaderSourceMappingNotFoundXml() {
        xmlMappingSourceService.setRdmMappingXmlPath("/another-rdm-mapping.xml");
        List<VersionAndFieldMapping> expectedEmptyVersionMappingList = Collections.emptyList();
        List<VersionAndFieldMapping> actualEmptyVersionMappingList = xmlMappingSourceService.getVersionAndFieldMappingList();

        Assertions.assertEquals(expectedEmptyVersionMappingList, actualEmptyVersionMappingList);
    }


    /**
     * Ситуация когда путь к *.xml задан некорректно
     */
    @Test
    public void testXmlMappingLoadError() {
        xmlMappingSourceService.setRdmMappingXmlPath("");
        Assertions.assertThrows(RdmException.class, () -> xmlMappingSourceService.getVersionAndFieldMappingList());
    }

}
