package ru.i_novus.ms.rdm.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookUpdaterException;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookVersionsDeterminator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefBookVersionsDeterminatorTest {

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private RdmSyncDao dao;

    @BeforeEach
    public void setUp() throws Exception {
        VersionMapping versionMapping = mock(VersionMapping.class);
        when(versionMapping.getMappingLastUpdated()).thenReturn(LocalDateTime.MIN);
        when(dao.getVersionMapping(any(), eq("CURRENT"))).thenReturn(versionMapping);
        when(syncSourceService.getRefBook(any(), any())).thenAnswer(invocationOnMock -> new RefBookVersion(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1), null, null, null, null));
    }

    /**
     * Ни одна версия не была загружена еще
     */
    @Test
    void testNoVersionLoaded() throws RefBookUpdaterException {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(generateVersions(code));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-2"), dao, syncSourceService);
        assertEquals(List.of("1", "2"), determinator.getVersions());
    }

    /**
     * одна версия уже загружена, а другая нет
     */
    @Test
    void testSomeVersionIsLoaded() throws RefBookUpdaterException {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(Collections.singletonList(new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), true)));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-2"), dao, syncSourceService);
        assertEquals(Collections.singletonList("2"), determinator.getVersions());
    }

    /**
     * Часть версий вне диапазона
     */
    @Test
    void testSomeVersionNotInRange() throws RefBookUpdaterException {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-1"), dao, syncSourceService);
        assertEquals(Collections.singletonList("1"), determinator.getVersions());
    }

    /**
     * все версии уже загружены
     */
    @Test
    void testAllVersionsLoaded() throws RefBookUpdaterException {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        assertTrue(determinator.getVersions().isEmpty());

    }

    /**
     * Диапазон на все версии
     */
    @Test
    void testLoadAllVersions() throws RefBookUpdaterException {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(generateVersions(code));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "*"), dao, syncSourceService);
        assertEquals(List.of("1", "2"), determinator.getVersions());
    }

    /**
     * Диапазон задан, все версии загружены, для всех один маппинг CURRENT и он меняется, тогда обновляем только актуальный
     */
    @Test
    void testAllVersionLoadedAndCurrentMappingChanged() throws RefBookUpdaterException {
        String code = "someCode";
        LocalDateTime now = LocalDateTime.now();
        List<RefBookVersionItem> versions = generateVersions(code);
        VersionMapping versionMapping = mock(VersionMapping.class);
        when(versionMapping.getMappingLastUpdated()).thenReturn(now.plusDays(1));
        when(dao.getVersionMapping(code, "CURRENT")).thenReturn(versionMapping);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, now.minusDays(1), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, now, true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        assertEquals(List.of("2"), determinator.getVersions());
    }

    @Test
    void testWhenRangeIsNull() throws RefBookUpdaterException {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getRefBook(code, null)).thenReturn(new RefBookVersion(generateVersions(code).get(1), null));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        assertEquals(List.of("2"), determinator.getVersions());
    }

    /**
     * Нет диапазона и последняя версия уже загружена
     */
    @Test
    void testWhenRangeIsNullAndHasLoadedVersion() throws RefBookUpdaterException {
        String code = "someCode";
        RefBookVersion refBookVersion = new RefBookVersion(generateVersions(code).get(1), null);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(2, code, refBookVersion.getVersion(),  refBookVersion.getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        assertTrue(determinator.getVersions().isEmpty());
    }

    /**
     * Нет диапазона и последняя версия загружена со старым маппингом
     */
    @Test
    void testWhenRangeIsNullAndHasLoadedVersionAndNewMapping() throws RefBookUpdaterException {
        String code = "someCode";
        RefBookVersion refBookVersion = new RefBookVersion(generateVersions(code).get(1), null);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime mappingLastUpdate = now.plus(1, ChronoUnit.DAYS);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(2, code, refBookVersion.getVersion(), refBookVersion.getFrom(), null, now, true)
        ));
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        VersionMapping versionMapping = mock(VersionMapping.class);
        when(versionMapping.getMappingLastUpdated()).thenReturn(mappingLastUpdate);
        when(dao.getVersionMapping(refBookVersion.getCode(), "CURRENT")).thenReturn(versionMapping);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        assertEquals(List.of("2"), determinator.getVersions());
    }

    /**
     * Нет диапазона, последняя версия загружена, обновлен текущий маппинг, но маппинг для версии тот же, поэтому не обновляем
     */
    @Test
    void testWhenRangeIsNullAndHasLoadedVersionAndOldMappingForSpecVersion() throws RefBookUpdaterException {
        String code = "someCode";
        RefBookVersion refBookVersion = new RefBookVersion(generateVersions(code).get(1), null);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(2, code, refBookVersion.getVersion(), refBookVersion.getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        VersionMapping specifyVersionMapping = mock(VersionMapping.class);
        when(specifyVersionMapping.getMappingLastUpdated()).thenReturn(LocalDateTime.MIN);
        when(dao.getVersionMapping(refBookVersion.getCode(), refBookVersion.getVersion())).thenReturn(specifyVersionMapping);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        assertTrue(determinator.getVersions().isEmpty());
    }

    @Test
    void testLoadRdmNotVersioned() throws RefBookUpdaterException {
        String code = "someCode";
        VersionMapping versionMapping = createVersionMapping(code,LocalDateTime.of(2022, 4, 1, 10, 0),  SyncTypeEnum.RDM_NOT_VERSIONED, null, false);

        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, "-1",  LocalDateTime.of(2022, 4, 1, 10, 0), null, LocalDateTime.now(), true)
        ));

        when(dao.getVersionMapping(any(), eq("CURRENT"))).thenReturn(versionMapping);
        RefBookVersion refBookVersion1 = new RefBookVersion();
        refBookVersion1.setVersion("-1");
        refBookVersion1.setVersionId(1);
        refBookVersion1.setStructure(new RefBookStructure());
        refBookVersion1.setFrom( LocalDateTime.of(2022, 5, 1, 10, 0));

        when(syncSourceService.getRefBook(any(), any())).thenReturn(refBookVersion1);

        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);

        assertEquals(Collections.singletonList("-1"), determinator.getVersions());
    }

    /**
     * включено обновление всех версий диапазона при изменении маппинга и маппинг изменился
     */
    @Test
    void testRefreshableRangeWhenMappingChanged() throws RefBookUpdaterException {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now().minusDays(2), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now().minusDays(1), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        when(dao.getVersionMapping(code, "CURRENT")).thenReturn(createVersionMapping(code, LocalDateTime.now(), SyncTypeEnum.SIMPLE_VERSIONED, "1-*", true));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        assertEquals(List.of("1", "2"), determinator.getVersions());
    }

    /**
     * включено обновление всех версий диапазона при изменении маппинга, но маппинг не изменился
     */
    @Test
    void testRefreshableRangeWhenMappingNotChanged() throws RefBookUpdaterException {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now().minusDays(2), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now().minusDays(1), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        when(dao.getVersionMapping(code, "CURRENT")).thenReturn(createVersionMapping(code, LocalDateTime.now().minusDays(3), SyncTypeEnum.SIMPLE_VERSIONED, "1-*", true));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        assertTrue(determinator.getVersions().isEmpty());
    }

    /**
     * включено обновление всех версий диапазона при изменении маппинга, но маппинг только для конкретной версии
     */
    @Test
    void testRefreshableRangeWhenMappingChangedForSpecifyVersion() throws RefBookUpdaterException {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now().minusDays(2), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now().minusDays(1), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        when(dao.getVersionMapping(code, "CURRENT")).thenReturn(createVersionMapping(code, LocalDateTime.now().minusDays(3), SyncTypeEnum.SIMPLE_VERSIONED, "1-*", true));
        when(dao.getVersionMapping(code, "1")).thenReturn(createVersionMapping(code, LocalDateTime.now(), SyncTypeEnum.SIMPLE_VERSIONED, "1-*", true));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        assertEquals(List.of("1"), determinator.getVersions());
    }

    // проверка что нет первичного ключа
    // проверка что нет изменений No changes.

    private List<RefBookVersionItem> generateVersions(String code) {
        RefBookVersionItem v1 = new RefBookVersionItem(code, "1", LocalDateTime.of(2022, 1, 1, 10, 0), LocalDateTime.of(2022, 2, 1, 10, 0), 1);
        RefBookVersionItem v2 = new RefBookVersionItem(code, "2", v1.getTo(), null, 2);
        return List.of(v1, v2);
    }

    private VersionMapping createVersionMapping(String code, LocalDateTime mappingLastUpdated, SyncTypeEnum type, String range, boolean refreshableRange) {
       return new VersionMapping(
               1,
               code,
               "someName",
               "1",
                "someTable",
               "id",
               "someSource",
               "id",
               "is_deleted",
               mappingLastUpdated,
                -1,
               1,
               type,
               range,
               true,
               refreshableRange
       );
    }

}
