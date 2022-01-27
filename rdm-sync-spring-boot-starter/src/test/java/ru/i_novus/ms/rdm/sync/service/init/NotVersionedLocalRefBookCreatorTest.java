package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotVersionedLocalRefBookCreatorTest {

    @InjectMocks
    private NotVersionedLocalRefBookCreator creator;

    @Mock
    private RdmSyncDao rdmSyncDao;

    @Mock
    private SyncSourceDao syncSourceDao;

    @Spy
    private Set<SyncSourceServiceFactory> syncSourceServiceFactorySet = new HashSet<>();

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @Mock
    private SyncSourceService syncSourceService;

    @Before
    public void setUp() {
        when(syncSourceServiceFactory.isSatisfied(any())).thenReturn(true);
        syncSourceServiceFactorySet.add(syncSourceServiceFactory);
        when(syncSourceServiceFactory.createService(any())).thenReturn(syncSourceService);
    }

    @Test
    public void testCreate() {
        Integer mappingId = 1;
        String code = "test.code";
        String refBookName = "test.name";
        String sourceCode = "TEST_SOURCE_CODE";
        List<FieldMapping> expectedFieldMappingList = List.of(new FieldMapping("id", "integer", "id"), new FieldMapping("name", "varchar", "name"));
        RefBookVersion refBook = new RefBookVersion();
        refBook.setCode(code);
        refBook.setStructure(
                new RefBookStructure(null,
                        Collections.singletonList("id"),
                        Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING)
                )
        );

        when(rdmSyncDao.lockRefBookForUpdate(eq(code), eq(true))).thenReturn(true);
        when(rdmSyncDao.insertVersionMapping(any())).thenReturn(mappingId);
        when(rdmSyncDao.getFieldMappings(eq(mappingId))).thenReturn(expectedFieldMappingList);
        when(syncSourceService.getRefBook(any(), any())).thenReturn(refBook);

        SyncSource source = new SyncSource("name", "code", "{}", "service");

        when(syncSourceDao.findByCode(any())).thenReturn(source);

        creator.create(code, refBookName, sourceCode, SyncTypeEnum.NOT_VERSIONED, null, , "_sync_rec_id", null);


        ArgumentCaptor<VersionMapping> mappingCaptor = ArgumentCaptor.forClass(VersionMapping.class);
        verify(rdmSyncDao, times(1)).insertVersionMapping(mappingCaptor.capture());
        Assert.assertEquals(code, mappingCaptor.getValue().getCode());
        Assert.assertEquals(refBookName, mappingCaptor.getValue().getRefBookName());
        Assert.assertEquals(-1, mappingCaptor.getValue().getMappingVersion());
        Assert.assertEquals("deleted_ts", mappingCaptor.getValue().getDeletedField());
        Assert.assertEquals("rdm.ref_test_code", mappingCaptor.getValue().getTable());
        Assert.assertEquals("id", mappingCaptor.getValue().getPrimaryField());
        Assert.assertEquals("TEST_SOURCE_CODE",mappingCaptor.getValue().getSource());
        Assert.assertEquals(SyncTypeEnum.NOT_VERSIONED,mappingCaptor.getValue().getType());

        verify(rdmSyncDao, times(1)).insertFieldMapping(eq(1), argThat(ignoreOrderEqList(expectedFieldMappingList)));

        verify(rdmSyncDao, times(1)).createSchemaIfNotExists("rdm");

        verify(rdmSyncDao, times(1))
                .createTableIfNotExists(
                        eq("rdm"),
                        eq("ref_test_code"),
                        argThat(ignoreOrderEqList(expectedFieldMappingList)),
                        eq("deleted_ts"),
                        eq("_sync_rec_id"));


    }

    /**
     * маппинг уже есть, поэтому игнорируем
     */
    @Test
    public void testIgnoreCreateWhenRefBookWasLoaded() {
        String code = "testCode";
        when(rdmSyncDao.getVersionMapping(code, "CURRENT")).thenReturn(mock(VersionMapping.class));
        creator.create(code, null, "someSource", SyncTypeEnum.NOT_VERSIONED, null, null, null);
        verify(rdmSyncDao, never()).insertVersionMapping(any());
        verify(rdmSyncDao, never()).insertVersionMapping(any());

    }



    private <T> ArgumentMatcher<List<T>> ignoreOrderEqList(List<T> expectedList) {
        return list -> list.size()==expectedList.size() && list.containsAll(expectedList) && expectedList.containsAll(list);
    }


}
