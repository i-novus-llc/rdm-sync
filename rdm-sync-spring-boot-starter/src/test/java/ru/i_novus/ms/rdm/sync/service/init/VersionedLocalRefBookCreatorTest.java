package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private Set<SyncSourceServiceFactory> syncSourceServiceFactorySet = new HashSet<>();

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private SyncSourceDao syncSourceDao;

    @BeforeEach
    public void setUp() {
        when(syncSourceServiceFactory.isSatisfied(any())).thenReturn(true);
        syncSourceServiceFactorySet.add(syncSourceServiceFactory);
        when(syncSourceServiceFactory.createService(any())).thenReturn(syncSourceService);
        when(syncSourceService.getRefBook(any(), any())).thenReturn(createRefBook("testCode"));
        when(syncSourceDao.findByCode(any())).thenReturn(mock(SyncSource.class));
    }

    @Test
    void testCreate() {
        String testCode = "test";
        String source = "some source";
        List<FieldMapping> fieldMappings = createFieldMappings();
        SyncMapping syncMapping = createVersionAndFieldMappingByRefBookCode(testCode);
        when(rdmSyncDao.getFieldMappings(syncMapping.getVersionMapping().getId())).thenReturn(fieldMappings);
        creator.create(syncMapping);
        syncMapping.getVersionMapping().setId(null);
        verify(rdmSyncDao, times(1)).insertVersionMapping(syncMapping.getVersionMapping());
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
        String source = "some source";
        List<FieldMapping> fieldMappings = createFieldMappings();

        SyncMapping syncMapping = createVersionAndFieldMappingByRefBookCode(testCode);
        when(rdmSyncDao.getVersionMapping(testCode, "CURRENT")).thenReturn(syncMapping.getVersionMapping());
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
        return new VersionMapping(1, testCode, null, "CURRENT", "rdm.ref_test", "test_pk_field", "someSource", "id", null, null, -1, null, SyncTypeEnum.SIMPLE_VERSIONED, null, true, false);
    }

    private RefBookVersion createRefBook(String refBookCode) {
        RefBookVersion refBook = new RefBookVersion();
        refBook.setCode(refBookCode);
        refBook.setVersion("1.0");
        refBook.setVersionId(1);
        refBook.setFrom(LocalDateTime.of(2021, 1, 1, 12, 0));
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
