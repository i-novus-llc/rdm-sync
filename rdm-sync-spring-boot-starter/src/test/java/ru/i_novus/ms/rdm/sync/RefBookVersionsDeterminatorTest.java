package ru.i_novus.ms.rdm.sync;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookVersionsDeterminator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefBookVersionsDeterminatorTest {

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private RdmSyncDao dao;

    @Before
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
    public void testNoVersionLoaded() {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(generateVersions(code));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-2"), dao, syncSourceService);
        Assert.assertEquals(List.of("1", "2"), determinator.getVersions());
    }

    /**
     * одна версия уже загружена, а другая нет
     */
    @Test
    public void testSomeVersionIsLoaded() {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(Collections.singletonList(new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), true)));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-2"), dao, syncSourceService);
        Assert.assertEquals(Collections.singletonList("2"), determinator.getVersions());
    }

    /**
     * Часть версий вне диапазона
     */
    @Test
    public void testSomeVersionNotInRange() {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-1"), dao, syncSourceService);
        Assert.assertEquals(Collections.singletonList("1"), determinator.getVersions());
    }

    /**
     * все версии уже загружены
     */
    @Test
    public void testAllVersionsLoaded() {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        Assert.assertTrue(determinator.getVersions().isEmpty());

    }

    /**
     * Диапазон на все версии
     */
    @Test
    public void testLoadAllVersions() {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(generateVersions(code));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "*"), dao, syncSourceService);
        Assert.assertEquals(List.of("1", "2"), determinator.getVersions());
    }

    /**
     * Диапазон задан, все версии загружены, для всех один маппинг CURRENT и он меняется, тогда обновляем только актуальный
     */
    @Test
    public void testAllVersionLoadedAndCurrentMappingChanged() {
        String code = "someCode";
        List<RefBookVersionItem> versions = generateVersions(code);
        VersionMapping versionMapping = mock(VersionMapping.class);
        when(versionMapping.getMappingLastUpdated()).thenReturn(versions.get(1).getFrom().plusDays(1));
        when(dao.getVersionMapping(code, "CURRENT")).thenReturn(versionMapping);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        Assert.assertEquals(List.of("2"), determinator.getVersions());
    }

    @Test
    public void testWhenRangeIsNull() {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getRefBook(code, null)).thenReturn(new RefBookVersion(generateVersions(code).get(1), null));
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        Assert.assertEquals(List.of("2"), determinator.getVersions());
    }

    /**
     * Нет диапазона и последняя версия уже загружена
     */
    @Test
    public void testWhenRangeIsNullAndHasLoadedVersion() {
        String code = "someCode";
        RefBookVersion refBookVersion = new RefBookVersion(generateVersions(code).get(1), null);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(2, code, refBookVersion.getVersion(),  refBookVersion.getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        Assert.assertTrue(determinator.getVersions().isEmpty());
    }

    /**
     * Нет диапазона и последняя версия загружена со старым маппингом
     */
    @Test
    public void testWhenRangeIsNullAndHasLoadedVersionAndNewMapping() {
        String code = "someCode";
        RefBookVersion refBookVersion = new RefBookVersion(generateVersions(code).get(1), null);
        LocalDateTime mappingLastUpdate = refBookVersion.getFrom().plus(1, ChronoUnit.DAYS);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(2, code, refBookVersion.getVersion(), refBookVersion.getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        VersionMapping versionMapping = mock(VersionMapping.class);
        when(versionMapping.getMappingLastUpdated()).thenReturn(mappingLastUpdate);
        when(dao.getVersionMapping(refBookVersion.getCode(), "CURRENT")).thenReturn(versionMapping);
        RefBookVersionsDeterminator determinator = new RefBookVersionsDeterminator(new SyncRefBook(1, code, null, null, null), dao, syncSourceService);
        Assert.assertEquals(List.of("2"), determinator.getVersions());
    }

    /**
     * Нет диапазона, последняя версия загружена, обновлен текущий маппинг, но маппинг для версии тот же, поэтому не обновляем
     */
    @Test
    public void testWhenRangeIsNullAndHasLoadedVersionAndOldMappingForSpecVersion() {
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
        Assert.assertTrue(determinator.getVersions().isEmpty());
    }

    @Test
    public void testLoadRdmNotVersioned(){
        String code = "someCode";
        VersionMapping versionMapping = new VersionMapping(1,code, "someName", "1",
                "someTable", "id", "RDM", "id", "is_deleted", LocalDateTime.of(2022, 4, 1, 10, 0),
                -1, 1, SyncTypeEnum.RDM_NOT_VERSIONED, null);

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

        Assert.assertEquals(Collections.singletonList("-1"), determinator.getVersions());
    }

    /**
     * нет диапазона версий и  нет изменений
     */
    @Test
    public void testNoChangesByCurrent() {

    }

    // проверка что нет первичного ключа
    // проверка что нет изменений No changes.
    //validateStructureAndMapping

    private List<RefBookVersionItem> generateVersions(String code) {
        RefBookVersionItem v1 = new RefBookVersionItem(code, "1", LocalDateTime.of(2022, 1, 1, 10, 0), LocalDateTime.of(2022, 2, 1, 10, 0), 1);
        RefBookVersionItem v2 = new RefBookVersionItem(code, "2", v1.getTo(), null, 2);
        return List.of(v1, v2);
    }

}
