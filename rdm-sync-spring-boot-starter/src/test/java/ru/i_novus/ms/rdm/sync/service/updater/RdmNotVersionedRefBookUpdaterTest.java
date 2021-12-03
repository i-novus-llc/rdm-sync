package ru.i_novus.ms.rdm.sync.service.updater;

import org.junit.Before;
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
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterServiceLocator;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RdmNotVersionedRefBookUpdaterTest {

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private PersisterServiceLocator persisterServiceLocator;

    @Mock
    private RdmLoggingService loggingService;

    @Mock
    private PersisterService persisterService;

    @InjectMocks
    private RdmNotVersionedRefBookUpdater updater;

    @Captor
    private ArgumentCaptor<RefBook> refBookArgumentCaptor;

    @Captor
    private ArgumentCaptor<VersionMapping> versionMappingArgumentCaptor;

    @Captor
    private ArgumentCaptor<SyncSourceService> syncSourceServiceArgumentCaptor;

    @Before
    public void setUp() {
        when(persisterServiceLocator.getPersisterService(any())).thenReturn(persisterService);
    }

    @Test
    public void testFirstWrite() {
        RefBook refBook = createRefbook();

        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(null);
        when(syncSourceService.getRefBook(anyString())).thenReturn(refBook);
        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(createVersionMapping());
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));

        updater.update(refBook.getCode());

        verify(persisterService).firstWrite(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBook expectedRefBook = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBook.getCode(), refBook.getCode());
    }

    @Test
    public void testUpdateWithChangeUpdateDate() {
        RefBook refBook = createRefbook();

        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(createLoadedVersion());
        when(syncSourceService.getRefBook(anyString())).thenReturn(refBook);
        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(createVersionMapping());
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));

        refBook.setLastPublishDate(LocalDateTime.of(2018, 1, 14, 10, 34));
        updater.update(refBook.getCode());
        verify(persisterService).repeatVersion(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBook expectedRefBook = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBook.getCode(), refBook.getCode());
    }

    @Test
    public void testUpdateWithChangeMapping() {
        RefBook refBook = createRefbook();

        VersionMapping versionMapping = createVersionMapping();
        versionMapping.setMappingLastUpdated(LocalDateTime.of(2018, 1, 14, 10, 34));

        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(createLoadedVersion());
        when(syncSourceService.getRefBook(anyString())).thenReturn(refBook);

        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));


        updater.update(refBook.getCode());
        verify(persisterService).repeatVersion(refBookArgumentCaptor.capture(), versionMappingArgumentCaptor.capture(), syncSourceServiceArgumentCaptor.capture());
        RefBook expectedRefBook = refBookArgumentCaptor.getValue();
        assertEquals(expectedRefBook.getCode(), refBook.getCode());
    }

    @Test
    public void testIgnoreUpdate() {

        RefBook refBook = createRefbook();
        LoadedVersion loadedVersion = createLoadedVersion();
        loadedVersion.setPublicationDate(refBook.getLastPublishDate());
        VersionMapping versionMapping = createVersionMapping();
        when(dao.getLoadedVersion(refBook.getCode())).thenReturn(loadedVersion);
        when(syncSourceService.getRefBook(anyString())).thenReturn(refBook);

        when(dao.getVersionMapping(refBook.getCode(), "CURRENT")).thenReturn(versionMapping);
        when(dao.getFieldMappings(refBook.getCode())).thenReturn(Arrays.asList(createFieldMapping()));

        updater.update(refBook.getCode());
        verifyNoMoreInteractions(persisterService);

    }

    private LoadedVersion createLoadedVersion() {
        return new LoadedVersion(1, "code", "-1", LocalDateTime.of(2017, 1, 14, 10, 34), LocalDateTime.of(2017, 1, 14, 10, 34));
    }

    private RefBook createRefbook() {
        RefBookStructure structure = new RefBookStructure();
        structure.setPrimaries(Arrays.asList("id"));
        structure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.STRING));

        RefBook refBook = new RefBook();
        refBook.setStructure(structure);
        refBook.setLastVersion("-1");
        refBook.setCode("code");
        refBook.setLastPublishDate(LocalDateTime.of(2017, 1, 14, 10, 34));
        refBook.setLastVersionId(1);

        return refBook;
    }

    private VersionMapping createVersionMapping() {
        return new VersionMapping(1, "code", "-1", "table", "source", "id", "", LocalDateTime.of(2017, 1, 14, 10, 34), -1, 1, SyncTypeEnum.NOT_VERSIONED);
    }

    private FieldMapping createFieldMapping() {
        return new FieldMapping("id", "dataType", "id");
    }

}