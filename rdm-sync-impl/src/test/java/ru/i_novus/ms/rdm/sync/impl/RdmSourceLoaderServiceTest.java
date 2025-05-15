package ru.i_novus.ms.rdm.sync.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceSavingDao;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:rdm-source-test.properties")
public class RdmSourceLoaderServiceTest {

    @Mock
    private SyncSourceSavingDao syncSourceDao;

    @Value("${rdm.backend.path}")
    private String url;

    private RdmSourceLoaderService sourceLoaderService;

    @BeforeEach
    public void setUp() throws Exception {
        sourceLoaderService = new RdmSourceLoaderService(url, syncSourceDao);
    }

    @Test
    public void testLoad() {
        SyncSource syncSourceActual = new SyncSource("RDM", "RDM", url, "RdmSyncSourceServiceFactory");
        sourceLoaderService.load();
        verify(syncSourceDao).save(eq(syncSourceActual));
    }
}