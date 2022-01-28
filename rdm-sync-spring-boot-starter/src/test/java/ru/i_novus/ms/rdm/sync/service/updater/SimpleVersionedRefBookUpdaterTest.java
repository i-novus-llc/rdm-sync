package ru.i_novus.ms.rdm.sync.service.updater;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleVersionedRefBookUpdaterTest extends AbstractRefBookUpdaterTest {

    @InjectMocks
    private SimpleVersionedRefBookUpdater updater;

    @Mock
    private PersisterService persisterService;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private SyncSourceService syncSourceService;

    private final String code = "testCode";

    @Before
    public void setUp() throws Exception {
        VersionMapping versionMapping = mock(VersionMapping.class);
        when(versionMapping.getPrimaryField()).thenReturn("id");
        when(dao.getVersionMapping(eq(code), eq("CURRENT"))).thenReturn(versionMapping);
        when(dao.getFieldMappings(anyInt())).thenReturn(List.of(new FieldMapping("id", "integer", "id")));
    }

    @Test
    public void testFirstWrite() {
        String version = "1.0";
        LocalDateTime pubDate = LocalDateTime.now();
        RefBookVersion refBookVersion = generateRefBookVersion(code, version, pubDate, null);

        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);

        updater.update(code, null);

        verify(dao, times(1)).insertLoadedVersion(code, version, pubDate, null, true);
        verify(persisterService, times(1)).firstWrite(eq(refBookVersion), any(), any());
    }

    @Test
    /**
     * Уже есть загруженная версия, грузим след версию
     */
    public void testLoadNextVersion() {
        String oldVersion = "1.0";
        String newVersion = "1.1";
        LocalDateTime oldVersionPubDate = LocalDateTime.of(2022, 1, 1, 11, 11);
        LocalDateTime newVersionPubDate = LocalDateTime.of(2022, 1, 20, 11, 11);


        when(dao.getActualLoadedVersion(code)).thenReturn(new LoadedVersion(1, code, oldVersion, oldVersionPubDate, null, oldVersionPubDate, true));
        RefBookVersion refBookVersion = generateRefBookVersion(code, newVersion, newVersionPubDate, null);
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        when(dao.existsLoadedVersion(code)).thenReturn(true);
        updater.update(code, null);

        verify(dao, times(1)).closeLoadedVersion(code, oldVersion, newVersionPubDate);
        verify(dao, times(1)).insertLoadedVersion(code, newVersion, newVersionPubDate, null, true);
        verify(persisterService, times(1)).merge(eq(refBookVersion), eq(oldVersion), any(), any());
    }

    /**
     * Для загруженной версии добавили специальный маппинг по ее версию
     */
    @Test
    public void testAddMappingForLoadedVersion() {

        String version = "1.0";
        LocalDateTime fromDate = LocalDateTime.of(2022, 1, 1, 11, 11);
        LocalDateTime toDate = LocalDateTime.of(2022, 1, 20, 11, 11);
        LocalDateTime syncDate = LocalDateTime.of(2022, 1, 20, 13, 11);
        LocalDateTime mappingUpdDate = LocalDateTime.of(2022, 2, 20, 13, 11);
        RefBookVersion refBookVersion = generateRefBookVersion(code, version, fromDate, toDate);
        LoadedVersion loadedVersion = new LoadedVersion(1, code, version, fromDate, toDate, syncDate, false);
        when(dao.getLoadedVersion(code, version)).thenReturn(loadedVersion);
        when(dao.existsLoadedVersion(code)).thenReturn(true);
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        VersionMapping versionMapping = new VersionMapping(5, code, null, version, "tbl" ,"","src", "id", null, mappingUpdDate, -1, null, null, null);
        when(dao.getVersionMapping(eq(code), eq(version))).thenReturn(versionMapping);

        updater.update(code, null);

        verify(persisterService, times(1)).repeatVersion(eq(refBookVersion), eq(versionMapping), any());
        verify(dao, times(1)).updateLoadedVersion(loadedVersion.getId(), refBookVersion.getVersion(), refBookVersion.getFrom(), refBookVersion.getTo());
    }

    /**
     * Есть актуальная версия, но теперь грузим предыдущую которой не было. Актуальной должна остаться та же
     */
    @Test
    public void testLoadPreviousVersion() {
        String actualVersion = "1.5";
        String previousVersion = "1.0";
        LocalDateTime actualVersionPubDate = LocalDateTime.of(2022, 1, 1, 11, 11);
        LocalDateTime previousVersionPubDate = actualVersionPubDate.minus(1, ChronoUnit.MONTHS);

        when(dao.getActualLoadedVersion(code)).thenReturn(new LoadedVersion(1, code, actualVersion, actualVersionPubDate, LocalDateTime.now(), actualVersionPubDate, true));
        RefBookVersion refBookVersion = generateRefBookVersion(code, previousVersion, previousVersionPubDate, null);
        when(syncSourceService.getRefBook(code, null)).thenReturn(refBookVersion);
        when(dao.existsLoadedVersion(code)).thenReturn(true);
        updater.update(code, null);

        verify(dao, never()).closeLoadedVersion(code, actualVersion, previousVersionPubDate);
        verify(dao, times(1)).insertLoadedVersion(code, previousVersion, previousVersionPubDate, null, false);
        verify(persisterService, times(1)).merge(eq(refBookVersion), eq(actualVersion), any(), any());

    }

    private RefBookVersion generateRefBookVersion(String code, String version, LocalDateTime pubDate, LocalDateTime closeDate) {
        RefBookStructure structure = new RefBookStructure(null, Collections.singletonList("id"), Map.of("id", AttributeTypeEnum.INTEGER));
        return new RefBookVersion(code, version, pubDate, null, 1, structure);
    }
}
