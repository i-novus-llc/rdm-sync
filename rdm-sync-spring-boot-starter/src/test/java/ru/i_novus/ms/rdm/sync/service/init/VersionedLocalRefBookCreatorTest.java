package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled("Не используется тестируемый класс")
class VersionedLocalRefBookCreatorTest {

    @InjectMocks
    private VersionedLocalRefBookCreator creator;

    @Mock
    private RdmSyncDao rdmSyncDao;

    @Spy
    private Set<SyncSourceServiceFactory> syncSourceServiceFactorySet;

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @Mock
    private VersionMappingService versionMappingService;

    @BeforeEach
    public void setUp() {
        syncSourceServiceFactorySet = new HashSet<>();
        syncSourceServiceFactorySet.add(syncSourceServiceFactory);
    }

    @Test
    void testCreate() {
        String testCode = "test";
        List<FieldMapping> fieldMappings = createFieldMappings();
        SyncMapping syncMapping = createVersionAndFieldMappingByRefBookCode(testCode);
        when(rdmSyncDao.getFieldMappings(syncMapping.getVersionMapping().getId())).thenReturn(fieldMappings);
        when(rdmSyncDao.insertVersionMapping(any())).thenReturn(syncMapping.getVersionMapping().getId());
        creator.create(syncMapping);
        syncMapping.getVersionMapping().setId(null);
        verify(rdmSyncDao, times(1)).createSchemaIfNotExists("rdm");

        verify(rdmSyncDao, times(1))
                .createVersionedTableIfNotExists(
                        "rdm",
                        "ref_test",
                        fieldMappings
                , "test_pk_field");
    }


    /**
     * создание таблицы при существующем маппинге
     */
    @Test
    void testCreateWithExistingMapping() {
        String testCode = "test";
        List<FieldMapping> fieldMappings = createFieldMappings();

        SyncMapping syncMapping = createVersionAndFieldMappingByRefBookCode(testCode);
        when(versionMappingService.getVersionMapping(any(), any())).thenReturn(syncMapping.getVersionMapping());
        when(rdmSyncDao.getFieldMappings(syncMapping.getVersionMapping().getId())).thenReturn(fieldMappings);

        creator.create(syncMapping);

        verify(rdmSyncDao, never()).insertVersionMapping(any());
        verify(rdmSyncDao, times(1)).createSchemaIfNotExists("rdm");

        verify(rdmSyncDao, times(1))
                .createVersionedTableIfNotExists(
                        "rdm",
                        "ref_test",
                        fieldMappings
                , "test_pk_field");

    }

    /**
     * игнорируем создание маппинга и таблицы если была загруженна хоть одна версия
     */
    @Test
    void testIgnoreCreateIfExistsLoadedVersion() {
        SyncMapping syncMapping = createVersionAndFieldMappingByRefBookCode("test");

        when(rdmSyncDao.existsLoadedVersion(any())).thenReturn(true);
        creator.create(syncMapping);
        verify(rdmSyncDao, never()).insertVersionMapping(any());
        verify(rdmSyncDao, never()).createSchemaIfNotExists(any());
        verify(rdmSyncDao, never()).createVersionedTableIfNotExists(any(), any(), any(), any());
    }

    private SyncMapping createVersionAndFieldMappingByRefBookCode(String refBookCode){
        return new SyncMapping(createVersionMapping(refBookCode), createFieldMappings());
    }

    private VersionMapping createVersionMapping(String testCode) {
        return new VersionMapping(1, testCode, null, "rdm.ref_test", "test_pk_field", "someSource", "id", null, null, -1, null, SyncTypeEnum.SIMPLE_VERSIONED, new Range("*"), true, false);
    }

    private List<FieldMapping> createFieldMappings() {
        return List.of(
                new FieldMapping("id", "integer", "id"),
                new FieldMapping("name", "varchar", "name")
        );
    }
}
