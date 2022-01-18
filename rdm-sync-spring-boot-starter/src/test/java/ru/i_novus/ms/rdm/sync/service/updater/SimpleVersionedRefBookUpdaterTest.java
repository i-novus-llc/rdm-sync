package ru.i_novus.ms.rdm.sync.service.updater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleVersionedRefBookUpdaterTest extends AbstractRefBookUpdaterTest {

    @InjectMocks
    private SimpleVersionedRefBookUpdater updater;

    @Mock
    private PersisterService persisterService;

    @Mock
    private RdmSyncDao rdmSyncDao;

    @Mock
    private SyncSourceService syncSourceService;


    @Test
    public void testFirstWrite() {
        String code = "code";
        when(rdmSyncDao.getVersionMapping(code, "CURRENT")).thenReturn(createVersionMapping(SyncTypeEnum.SIMPLE_VERSIONED));
        when(rdmSyncDao.getLoadedVersion(code)).thenReturn(null);
        RefBookVersion refBookVersion = createRefbook();
        when(rdmSyncDao.getFieldMappings(code)).thenReturn(Arrays.asList(createFieldMapping()));
        when(syncSourceService.getRefBook(eq(code), any())).thenReturn(refBookVersion);
        updater.update(code);
        verify(persisterService, times(1)).firstWrite(eq(refBookVersion), any(), eq(syncSourceService));
    }
}
