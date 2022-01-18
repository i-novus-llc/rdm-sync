package ru.i_novus.ms.rdm.sync.service.updater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

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

        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(null);
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);
        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED));
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));

        updater.update(refBook.getCode());

        verify(persisterService).firstWrite(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBookVersion expectedRefBook = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBook.getCode(), refBook.getCode());
    }

    @Test
    public void testUpdateWithChangeUpdateDate() {
        RefBookVersion refBook = createRefbook();

        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(createLoadedVersion());
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);
        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED));
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));

        refBook.setFrom(LocalDateTime.of(2018, 1, 14, 10, 34));
        updater.update(refBook.getCode());
        verify(persisterService).repeatVersion(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBookVersion expectedRefBook = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBook.getCode(), refBook.getCode());
    }

    @Test
    public void testUpdateWithChangeMapping() {
        RefBookVersion refBook = createRefbook();

        VersionMapping versionMapping = createVersionMapping(SyncTypeEnum.RDM_NOT_VERSIONED);
        versionMapping.setMappingLastUpdated(LocalDateTime.of(2018, 1, 14, 10, 34));

        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(createLoadedVersion());
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);

        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));


        updater.update(refBook.getCode());
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
        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(loadedVersion);
        when(syncSourceService.getRefBook(anyString(), any())).thenReturn(refBook);

        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));

        updater.update(refBook.getCode());
        verifyNoMoreInteractions(persisterService);

    }




}