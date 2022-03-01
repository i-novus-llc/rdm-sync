package ru.i_novus.ms.rdm.sync.service.updater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RdmNotVersionedRefBookUpdaterTest extends AbstractRefBookUpdaterTest {

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private PersisterService persisterService;

    @InjectMocks
    private RdmNotVersionedRefBookUpdater updater;

    @Captor
    private ArgumentCaptor<RefBookVersion> refBookArgumentCaptor;

    @Captor
    private ArgumentCaptor<VersionMapping> versionMappingArgumentCaptor;

    @Captor
    private ArgumentCaptor<SyncSourceService> syncSourceServiceArgumentCaptor;

    @Test
    public void testFirstWrite() {
        RefBookVersion refBook = createRefbook();

        when(dao.existsLoadedVersion(refBook.getCode())).thenReturn(false);
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);
        VersionMapping versionMapping = createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED);
        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(Arrays.asList(createFieldMapping()));

        updater.update(refBook.getCode(), null);

        verify(persisterService).firstWrite(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBookVersion expectedRefBook = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBook.getCode(), refBook.getCode());
    }

    /**
     * изменились данные справочника
     */
    @Test
    public void testUpdateWithChangeUpdateDate() {
        RefBookVersion refBook = createRefbook();

        LoadedVersion loadedVersion = createLoadedVersion();
        when(dao.getLoadedVersion(refBook.getCode(), refBook.getVersion())).thenReturn(loadedVersion);
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);
        VersionMapping versionMapping = createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED);
        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(Arrays.asList(createFieldMapping()));

        //изменилась дата публикации
        refBook.setFrom(loadedVersion.getPublicationDate().plus(5, ChronoUnit.DAYS));
        updater.update(refBook.getCode(), null);
        verify(persisterService).repeatVersion(eq(refBook), eq(versionMapping), any());
        verify(dao, times(1)).updateLoadedVersion(loadedVersion.getId(), refBook.getVersion(), refBook.getFrom(), refBook.getTo());

    }

    @Test
    public void testUpdateWithChangeMapping() {
        RefBookVersion refBook = createRefbook();

        VersionMapping versionMapping = createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED);
        versionMapping.setMappingLastUpdated(LocalDateTime.of(2018, 1, 14, 10, 34));

        when(dao.getLoadedVersion(refBook.getCode(), refBook.getVersion())).thenReturn(createLoadedVersion());
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);

        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(Arrays.asList(createFieldMapping()));


        updater.update(refBook.getCode(), null);
        verify(persisterService).repeatVersion(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBookVersion expectedRefBookVersion = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBookVersion.getCode(), refBook.getCode());
    }

    @Test
    public void testIgnoreUpdate() {

        RefBookVersion refBook = createRefbook();
        LoadedVersion loadedVersion = createLoadedVersion();
        loadedVersion.setPublicationDate(refBook.getFrom());
        VersionMapping versionMapping = createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED);
        when(dao.getLoadedVersion(refBook.getCode(), refBook.getVersion())).thenReturn(loadedVersion);
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);

        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(Arrays.asList(createFieldMapping()));

        updater.update(refBook.getCode(), null);
        verifyNoMoreInteractions(persisterService);

    }




}