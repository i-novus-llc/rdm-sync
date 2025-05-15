package ru.i_novus.ms.fnsi.sync.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceSavingDao;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FnsiSourceProperty.class)
@EnableConfigurationProperties(FnsiSourceProperty.class)
@TestPropertySource("classpath:fnsi-source-test.properties")
public class FnsiSourceLoaderServiceTest {

    @Autowired
    FnsiSourceProperty fnsiSourceProperty;

    @Mock
    private SyncSourceSavingDao syncSourceDao;

    private FnsiSourceLoaderService sourceLoaderService;

    @BeforeEach
    public void setUp() throws Exception {
        sourceLoaderService = new FnsiSourceLoaderService(fnsiSourceProperty, syncSourceDao);
    }

    @Test
    public void testLoad() {
        SyncSource actual1 = new SyncSource("a", "RU_FNSI", "{\"userKey\":\"qwerty\", \"url\":\"http://fnsi.ru\"}", "FnsiSyncSourceServiceFactory");
        SyncSource actual2 = new SyncSource("b", "WORLD_FNSI", "{\"userKey\":\"asd\", \"url\":\"http://fnsi.com\"}", "FnsiSyncSourceServiceFactory");
        sourceLoaderService.load();
        verify(syncSourceDao, times(1)).save(eq(actual1));
        verify(syncSourceDao, times(1)).save(eq(actual2));
    }

}