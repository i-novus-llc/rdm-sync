package ru.i_novus.ms.rdm.sync.init.description;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.init.dao.SyncSourceDao;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.STRING;

@ExtendWith(MockitoExtension.class)
class RefBookDescriptionServiceImplTest {

    private static final String TEST_SOURCE_CODE = "TEST_SOURCE";

    private RefBookDescriptionServiceImpl descriptionService;
    
    @Mock
    private SyncSourceDao syncSourceDao;

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private SyncSourceServiceFactory syncSourceServiceFactory;

    @BeforeEach
    void setUp() {
        descriptionService = new RefBookDescriptionServiceImpl(
                syncSourceDao,
                Set.of(syncSourceServiceFactory),
                EnrichCommentsMode.ALWAYS,
                3
        );
    }

    @Test
    void testWhenMappingIsForAllVersionsThanGetDescriptionFromLastVersion() {
        prepareSyncSourceMocks();
        String code = "testCode";
        RefBookDescription expected = new RefBookDescription("some refBook", Map.of("id", "identity", "name", "some name"));
        String version = "1";
        when(syncSourceService.getVersions(anyString())).thenReturn(List.of(new RefBookVersionItem(code, version, LocalDateTime.now(), null, null)));
        RefBookStructure structure = getStructure(expected.refDescription(), expected.attributeDescriptions());
        when(syncSourceService.getRefBook(code, version)).thenReturn(new RefBookVersion(code, version, null, null, null, structure));
        RefBookDescription refBookDescription = descriptionService.getRefBookDescription(
                new SyncMapping(VersionMapping.builder().code(code).source(TEST_SOURCE_CODE).build(), getFieldMappings(expected))
        );
        assertEquals(expected, refBookDescription);
    }

    /**
     * Атрибуты содержатся в двух версиях
     */
    @Test
    void testGetDescriptionFromTwoVersion() {
        prepareSyncSourceMocks();
        String code = "testCode";
        String version1 = "1";
        String version2 = "2";
        RefBookDescription expected = new RefBookDescription(
                "some refBook",
                Map.of("id", "identity", "name", "some name", "shortName", "some short name")
        );
        List<FieldMapping> fieldMappings = getFieldMappings(expected);
        when(syncSourceService.getVersions(anyString())).thenReturn(List.of(
                new RefBookVersionItem(code, version1, LocalDateTime.now(), null, null),
                new RefBookVersionItem(code, version2, LocalDateTime.now(), null, null)
        ));
        RefBookStructure structure1 = getStructure(expected.refDescription(), Map.of("id", "identity", "name", "some name"));
        RefBookStructure structure2 = getStructure(expected.refDescription(), Map.of("id", "identity", "shortName", "some short name"));
        when(syncSourceService.getRefBook(code, version1))
                .thenReturn(
                        new RefBookVersion(
                                code,
                                version1,
                                LocalDateTime.now().minus(1, ChronoUnit.DAYS),
                                null,
                                null,
                                structure1
                        )
                );
        when(syncSourceService.getRefBook(code, version2))
                .thenReturn(
                        new RefBookVersion(code, version2, LocalDateTime.now(), null, null, structure2)
                );

        RefBookDescription refBookDescription = descriptionService.getRefBookDescription(new SyncMapping(VersionMapping.builder().code(code).source(TEST_SOURCE_CODE).build(), fieldMappings));
        assertEquals(expected, refBookDescription);

    }

    @Test
    void testWhenMappingContainsAllDescriptionsThenUseDescriptionsFormMapping() {
        String code = "testCode";
        RefBookDescription expected = new RefBookDescription(
                "some refBook",
                Map.of("id", "identity", "name", "some name")
        );
        testGetDescriptionsWithoutFetch(expected);
    }

    @Test
    void testWhenMappingHasNoDescriptionForRefBookThenFetchAbsentDescription() {
        prepareSyncSourceMocks();
        String code = "testCode";
        RefBookDescription expected = new RefBookDescription(
                "some refBook",
                Map.of("id", "identity", "name", "some name")
        );
        String version = "1";
        when(syncSourceService.getVersions(anyString())).thenReturn(List.of(new RefBookVersionItem(code, version, LocalDateTime.now(), null, null)));
        RefBookStructure structure = getStructure(expected.refDescription(), expected.attributeDescriptions());
        when(syncSourceService.getRefBook(code, version)).thenReturn(new RefBookVersion(code, version, null, null, null, structure));
        RefBookDescription refBookDescription = descriptionService.getRefBookDescription(
                new SyncMapping(
                        VersionMapping.builder().code(code).source(TEST_SOURCE_CODE).build(),
                        expected.attributeDescriptions().keySet().stream()
                                .map(attr -> {
                                    FieldMapping fieldMapping = new FieldMapping(attr, "varchar", attr);
                                    fieldMapping.setComment(expected.attributeDescriptions().get(attr));
                                    return fieldMapping;
                                })
                                .toList()
                )
        );
        assertEquals(expected, refBookDescription);
    }

    @Test
    void testWhenMappingHasNoDescriptionForFieldThenFetchAbsentDescription() {
        prepareSyncSourceMocks();
        String code = "testCode";
        RefBookDescription expected = new RefBookDescription(
                "some refBook",
                Map.of("id", "identity", "name", "some name")
        );
        String version = "1";
        when(syncSourceService.getVersions(anyString())).thenReturn(List.of(new RefBookVersionItem(code, version, LocalDateTime.now(), null, null)));
        RefBookStructure structure = getStructure(expected.refDescription(), expected.attributeDescriptions());
        when(syncSourceService.getRefBook(code, version)).thenReturn(new RefBookVersion(code, version, null, null, null, structure));
        RefBookDescription refBookDescription = descriptionService.getRefBookDescription(
                new SyncMapping(
                        VersionMapping.builder().code(code).refBookName(expected.refDescription()).source(TEST_SOURCE_CODE).build(),
                        expected.attributeDescriptions().keySet().stream()
                                .map(attr -> {
                                    FieldMapping fieldMapping = new FieldMapping(attr, "varchar", attr);
                                    if (!attr.equals("name")) {
                                        fieldMapping.setComment(expected.attributeDescriptions().get(attr));
                                    }
                                    return fieldMapping;
                                })
                                .toList()
                )
        );
        assertEquals(expected, refBookDescription);
    }

    @Test
    void testWhenEnrichModeNeverThenUseDescriptionsFormMapping() {
        descriptionService = new RefBookDescriptionServiceImpl(
                syncSourceDao,
                Set.of(syncSourceServiceFactory),
                EnrichCommentsMode.NEVER,
                3
        );
        RefBookDescription expected = new RefBookDescription(null, Map.of("id", "identity"));
        testGetDescriptionsWithoutFetch(expected);
    }

    @Test
    void testWhenEnrichModeOnCreateAndTableNotExistsThenFetchAbsentDescription() {
        descriptionService = new RefBookDescriptionServiceImpl(
                syncSourceDao,
                Set.of(syncSourceServiceFactory),
                EnrichCommentsMode.ON_CREATE,
                3
        );
        prepareSyncSourceMocks();
        String code = "testCode";
        RefBookDescription expected = new RefBookDescription(
                "some refBook",
                Map.of("id", "identity", "name", "some name")
        );
        String version = "1";
        when(syncSourceService.getVersions(anyString())).thenReturn(List.of(new RefBookVersionItem(code, version, LocalDateTime.now(), null, null)));
        RefBookStructure structure = getStructure(expected.refDescription(), expected.attributeDescriptions());
        when(syncSourceService.getRefBook(code, version)).thenReturn(new RefBookVersion(code, version, null, null, null, structure));
        RefBookDescription refBookDescription = descriptionService.getRefBookDescription(
                new SyncMapping(
                        VersionMapping.builder().code(code).refBookName(expected.refDescription()).source(TEST_SOURCE_CODE).build(),
                        expected.attributeDescriptions().keySet().stream()
                                .map(attr -> {
                                    FieldMapping fieldMapping = new FieldMapping(attr, "varchar", attr);
                                    if (!attr.equals("name")) {
                                        fieldMapping.setComment(expected.attributeDescriptions().get(attr));
                                    }
                                    return fieldMapping;
                                })
                                .toList()
                )
        );
        assertEquals(expected, refBookDescription);
    }

    @Test
    void testWhenEnrichModeOnCreateAndTableExistsThenUseDescriptionsFormMapping() {
        descriptionService = new RefBookDescriptionServiceImpl(
                syncSourceDao,
                Set.of(syncSourceServiceFactory),
                EnrichCommentsMode.ON_CREATE,
                3
        );
        String table = "testTable";
        when(syncSourceDao.tableExists(eq(table))).thenReturn(true);
        RefBookDescription expected = new RefBookDescription(null, Map.of("id", "identity"));
        testGetDescriptionsWithoutFetch(expected, table);
    }

    private void testGetDescriptionsWithoutFetch(RefBookDescription expected) {
        testGetDescriptionsWithoutFetch(expected, "testTable");
    }

    private void testGetDescriptionsWithoutFetch(RefBookDescription expected, String table) {
        RefBookDescription refBookDescription = descriptionService.getRefBookDescription(
                new SyncMapping(
                        VersionMapping.builder().code("testCode").table(table).refBookName(expected.refDescription()).source(TEST_SOURCE_CODE).build(),
                        expected.attributeDescriptions().keySet().stream()
                                .map(attr -> {
                                    FieldMapping fieldMapping = new FieldMapping(attr, "varchar", attr);
                                    fieldMapping.setComment(expected.attributeDescriptions().get(attr));
                                    return fieldMapping;
                                })
                                .toList()
                )
        );
        assertEquals(expected, refBookDescription);
    }

    private void prepareSyncSourceMocks() {
        when(syncSourceDao.findByCode(TEST_SOURCE_CODE))
                .thenReturn(new SyncSource(TEST_SOURCE_CODE, TEST_SOURCE_CODE, "testValues", "RdmSyncSourceServiceFactory"));
        when(syncSourceServiceFactory.isSatisfied(any(SyncSource.class))).thenReturn(true);
        when(syncSourceServiceFactory.createService(any(SyncSource.class))).thenReturn(syncSourceService);
    }

    private List<FieldMapping> getFieldMappings(RefBookDescription description) {
        return description.attributeDescriptions().keySet().stream()
                .map(attr -> new FieldMapping(attr, "varchar", attr))
                .toList();
    }

    @NotNull
    private RefBookStructure getStructure(String refDescription, Map<String, String> attributeDescription) {
        Set<RefBookStructure.Attribute> attributes = attributeDescription.entrySet().stream()
                .map(entry -> new RefBookStructure.Attribute(entry.getKey(), STRING, entry.getValue()))
                .collect(Collectors.toSet());
        RefBookStructure structure = new RefBookStructure(
                Collections.emptyList(),
                Collections.emptyList(),
                attributes
        );
        structure.setRefDescription(refDescription);
        return structure;
    }
}