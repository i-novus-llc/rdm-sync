package ru.i_novus.ms.rdm.sync.service.mapping;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.model.loader.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * Тест кейсы для лоадера источника маппинга из *.properties
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AutoCreateRefBookProperty.class)
@EnableConfigurationProperties(AutoCreateRefBookProperty.class)
@TestPropertySource("classpath:mapping-sources/test-mapping-source.properties")
public class PropMappingSourceServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SyncSourceDao syncSourceDao;

    @Mock
    private RdmSyncDao rdmSyncDao;

    @Spy
    private Set<SyncSourceServiceFactory> syncSourceServiceFactorySet = new HashSet<>();

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @Mock
    private SyncSourceService syncSourceService;

    @Autowired
    private AutoCreateRefBookProperty autoCreateRefBookProperty;

    private Boolean caseIgnore = false;

    private String defaultSchema = "rdm";

    private PropMappingSourceService propMappingSourceService;

    @Before
    public void setUp() throws Exception {
        when(syncSourceServiceFactory.isSatisfied(any())).thenReturn(true);
        syncSourceServiceFactorySet.add(syncSourceServiceFactory);
        when(syncSourceServiceFactory.createService(any())).thenReturn(syncSourceService);

        propMappingSourceService = new PropMappingSourceService(
                syncSourceDao, rdmSyncDao, syncSourceServiceFactorySet,
                caseIgnore, defaultSchema, autoCreateRefBookProperty);


    }

    /**
     * Получение списка {@link VersionMapping} из *.properties
     */
    @Test
    public void testGetVersionMappingListFromProperties() {

        RefBookStructure structure = new RefBookStructure(null, List.of("id"), Map.of("name", AttributeTypeEnum.STRING));
        RefBookVersion refBookVersion = new RefBookVersion("EK003", "1", null, null, null, structure);

        when(syncSourceService.getRefBook(any(), any())).thenReturn(refBookVersion);

        VersionMapping expectedVersionMapping = MappingCreator.createVersionMapping();
        VersionAndFieldMapping actualVersionMapping = propMappingSourceService.getVersionAndFieldMappingList().get(0);

        Assert.assertEquals(expectedVersionMapping.getCode(), actualVersionMapping.getVersionMapping().getCode());
    }

    /**
     * Ситуация когда лоадер источника маппинга не нашел справочник, в этом случае должен вернуться пустой список
     */
    @Test
    public void testLoaderSourceMappingWithoutRefbooks() {
        AutoCreateRefBookProperty autoCreateRefBookPropertyWithoutRefbooks = new AutoCreateRefBookProperty();
        PropMappingSourceService propMappingSourceServiceWithoutRefbooks =
                new PropMappingSourceService(syncSourceDao, rdmSyncDao,syncSourceServiceFactorySet,
                        caseIgnore, defaultSchema, autoCreateRefBookPropertyWithoutRefbooks);

        List<VersionAndFieldMapping> actualEmptyVersionMappingList = propMappingSourceServiceWithoutRefbooks
                .getVersionAndFieldMappingList();
        List<VersionAndFieldMapping> expectedEmptyVersionMappingList = Collections.emptyList();

        Assert.assertEquals(expectedEmptyVersionMappingList, actualEmptyVersionMappingList);
    }


}
