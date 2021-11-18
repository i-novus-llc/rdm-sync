package ru.i_novus.ms.rdm.sync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.init.VersionedLocalRefBookCreator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionedLocalRefBookCreatorTest {

    @InjectMocks
    private VersionedLocalRefBookCreator creator;

    @Mock
    private RdmSyncDao rdmSyncDao;

    @Spy
    private Set<SyncSourceServiceFactory> syncSourceServiceFactorySet = new HashSet<>();

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private SyncSourceDao syncSourceDao;

    @Before
    public void setUp() {
        when(syncSourceServiceFactory.isSatisfied(any())).thenReturn(true);
        syncSourceServiceFactorySet.add(syncSourceServiceFactory);
        when(syncSourceServiceFactory.createService(any())).thenReturn(syncSourceService);
        when(syncSourceService.getRefBook(any())).thenReturn(createRefBook("testCode"));
        when(syncSourceDao.findByCode(any())).thenReturn(mock(SyncSource.class));
    }

    @Test
    public void testCreate() {
        String testCode = "test";
        String source = "some source";
        List<FieldMapping> fieldMappings = createFieldMappings();
        VersionMapping versionMapping = createVersionMapping(testCode);
        when(rdmSyncDao.getFieldMappings(testCode)).thenReturn(fieldMappings);
        creator.create(testCode, source);
        verify(rdmSyncDao, times(1)).insertVersionMapping(versionMapping);
        verify(rdmSyncDao, times(1)).createSchemaIfNotExists("rdm");

        verify(rdmSyncDao, times(1))
                .createVersionedTableIfNotExists(
                        "rdm",
                        "ref_test",
                        fieldMappings
                );
    }


    /**
     * создание таблицы при существующем маппинге
     */
    @Test
    public void testCreateWithExistingMapping() {
        String testCode = "test";
        String source = "some source";
        List<FieldMapping> fieldMappings = createFieldMappings();

        when(rdmSyncDao.getVersionMapping(testCode, "CURRENT")).thenReturn(createVersionMapping(testCode));
        when(rdmSyncDao.getFieldMappings(testCode)).thenReturn(fieldMappings);

        creator.create(testCode, source);

        verify(rdmSyncDao, never()).insertVersionMapping(any());
        verify(rdmSyncDao, times(1)).createSchemaIfNotExists("rdm");

        verify(rdmSyncDao, times(1))
                .createVersionedTableIfNotExists(
                        "rdm",
                        "ref_test",
                        fieldMappings
                );

    }

    private VersionMapping createVersionMapping(String testCode) {
        return new VersionMapping(null, testCode, "CURRENT", "rdm.ref_test", "someSource", "id", null, null, -1, null, SyncTypeEnum.VERSIONED);
    }

    private RefBook createRefBook(String refBookCode) {
        RefBook refBook = new RefBook();
        refBook.setCode(refBookCode);
        refBook.setLastVersion("1.0");
        refBook.setLastVersionId(1);
        refBook.setLastPublishDate(LocalDateTime.of(2021, 1, 1, 12, 0));
        refBook.setStructure(new RefBookStructure(null, List.of("id"), Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING)));
        return refBook;
    }

    private List<FieldMapping> createFieldMappings() {
        return List.of(
                new FieldMapping("id", "integer", "id"),
                new FieldMapping("name", "varchar", "name")
        );
    }
}
