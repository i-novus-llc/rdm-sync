package ru.i_novus.ms.rdm.sync;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookVersionIterator;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefBookVersionIteratorTest {

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private RdmSyncDao dao;

    /**
     * Ни одна версия не была загружена еще
     */
    @Test
    public void testNoVersionLoaded() {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(generateVersions(code));
        RefBookVersionIterator iterator = new RefBookVersionIterator(new SyncRefBook(1, code, null, null, "1-2"), dao, syncSourceService);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("1", iterator.next());
        Assert.assertEquals("2", iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    /**
     * одна версия уже загружена, а другая нет
     */
    @Test
    public void testSomeVersionIsLoaded() {
        String code = "someCode";
        List<RefBookVersion> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(Collections.singletonList(new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), true)));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionIterator iterator = new RefBookVersionIterator(new SyncRefBook(1, code, null, null, "1-2"), dao, syncSourceService);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("2", iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    /**
     * Часть версий вне диапазона
     */
    @Test
    public void testSomeVersionNotInRange() {
        String code = "someCode";
        List<RefBookVersion> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionIterator iterator = new RefBookVersionIterator(new SyncRefBook(1, code, null, null, "1-1"), dao, syncSourceService);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("1", iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    /**
     * все версии уже загружены
     */
    @Test
    public void testAllVersionsLoaded() {
        String code = "someCode";
        List<RefBookVersion> versions = generateVersions(code);
        when(dao.getLoadedVersions(any())).thenReturn(List.of(
                new LoadedVersion(1, code, versions.get(0).getVersion(),  versions.get(0).getFrom(), null, LocalDateTime.now(), null),
                new LoadedVersion(2, code, versions.get(1).getVersion(),  versions.get(1).getFrom(), null, LocalDateTime.now(), true)
        ));
        when(syncSourceService.getVersions(any())).thenReturn(versions);
        RefBookVersionIterator iterator = new RefBookVersionIterator(new SyncRefBook(1, code, null, null, "1-*"), dao, syncSourceService);
        Assert.assertFalse(iterator.hasNext());

    }

    /**
     * Диапазон на все версии
     */
    @Test
    public void testLoadAllVersions() {
        String code = "someCode";
        when(dao.getLoadedVersions(any())).thenReturn(Collections.emptyList());
        when(syncSourceService.getVersions(any())).thenReturn(generateVersions(code));
        RefBookVersionIterator iterator = new RefBookVersionIterator(new SyncRefBook(1, code, null, null, "*"), dao, syncSourceService);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("1", iterator.next());
        Assert.assertEquals("2", iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    private List<RefBookVersion> generateVersions(String code) {
        RefBookVersion v1 = new RefBookVersion(code, "1", LocalDateTime.of(2022, 1, 1, 10, 0), LocalDateTime.of(2022, 2, 1, 10, 0), 1, null);
        RefBookVersion v2 = new RefBookVersion(code, "2", v1.getTo(), null, 2, null);
        return List.of(v1, v2);
    }

}
