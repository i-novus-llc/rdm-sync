package ru.i_novus.ms.rdm.sync.init.mapping;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.init.loader.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.init.mapping.utils.MappingCreator;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * Тест кейсы для лоадера источника маппинга из *.properties
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AutoCreateRefBookProperty.class)
@EnableConfigurationProperties(AutoCreateRefBookProperty.class)
@TestPropertySource("classpath:mapping-sources/test-mapping-source.properties")
class PropMappingSourceServiceTest {

    @Mock
    private SyncSourceDao syncSourceDao;

    @Spy
    private Set<SyncSourceServiceFactory> syncSourceServiceFactorySet;

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @Mock
    private SyncSourceService syncSourceService;

    @Autowired
    private AutoCreateRefBookProperty autoCreateRefBookProperty;

    private Boolean caseIgnore = false;

    private String defaultSchema = "rdm";

    private PropMappingSourceService propMappingSourceService;

    @BeforeEach
    public void setUp() throws Exception {
        when(syncSourceServiceFactory.isSatisfied(any())).thenReturn(true);
        syncSourceServiceFactorySet = new HashSet<>();
        syncSourceServiceFactorySet.add(syncSourceServiceFactory);
        when(syncSourceServiceFactory.createService(any())).thenReturn(syncSourceService);

        propMappingSourceService = new PropMappingSourceService(
                syncSourceDao, syncSourceServiceFactorySet,
                caseIgnore, defaultSchema, autoCreateRefBookProperty);


    }

    /**
     * Получение списка {@link VersionMapping} из *.properties
     */
    @Test
    void testGetVersionMappingListFromProperties() {

        RefBookStructure structure = new RefBookStructure(
                null,
                List.of("id"),
                Set.of(new RefBookStructure.Attribute("name", AttributeTypeEnum.STRING, "наименование"))
        );
        RefBookVersion refBookVersion = new RefBookVersion("EK003", "1", null, null, null, structure);

        when(syncSourceService.getRefBook(any(), any())).thenReturn(refBookVersion);

        VersionMapping expectedVersionMapping = MappingCreator.createVersionMapping();
        SyncMapping actualVersionMapping = propMappingSourceService.getMappings().get(0);

        Assertions.assertEquals(expectedVersionMapping.getCode(), actualVersionMapping.getVersionMapping().getCode());
    }

    /**
     * Ситуация когда лоадер источника маппинга не нашел справочник, в этом случае должен вернуться пустой список
     */
    @Test
    void testLoaderSourceMappingWithoutRefbooks() {
        AutoCreateRefBookProperty autoCreateRefBookPropertyWithoutRefbooks = new AutoCreateRefBookProperty();
        PropMappingSourceService propMappingSourceServiceWithoutRefbooks =
                new PropMappingSourceService(syncSourceDao, syncSourceServiceFactorySet,
                        caseIgnore, defaultSchema, autoCreateRefBookPropertyWithoutRefbooks);

        List<SyncMapping> actualEmptyVersionMappingList = propMappingSourceServiceWithoutRefbooks
                .getMappings();
        List<SyncMapping> expectedEmptyVersionMappingList = Collections.emptyList();

        Assertions.assertEquals(expectedEmptyVersionMappingList, actualEmptyVersionMappingList);
    }


}
