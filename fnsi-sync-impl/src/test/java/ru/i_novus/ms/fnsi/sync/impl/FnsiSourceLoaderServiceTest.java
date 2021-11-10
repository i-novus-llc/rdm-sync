package ru.i_novus.ms.fnsi.sync.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FnsiSourceProperty.class)
@EnableConfigurationProperties(FnsiSourceProperty.class)
@TestPropertySource("classpath:fnsi-source-test.properties")
public class FnsiSourceLoaderServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Autowired
    FnsiSourceProperty fnsiSourceProperty;

    @Mock
    private SyncSourceDao syncSourceDao;

    private FnsiSourceLoaderService sourceLoaderService;

    @Before
    public void setUp() throws Exception {
        sourceLoaderService = new FnsiSourceLoaderService(fnsiSourceProperty, syncSourceDao);
    }

    @Test
    public void testLoad() {
        SyncSource actual1 = new SyncSource("a", "RU_FNSI", "{\"userKey\":\"qwerty\", \"url\":\"http://fnsi.ru\"}");
        SyncSource actual2 = new SyncSource("b", "WORLD_FNSI", "{\"userKey\":\"asd\", \"url\":\"http://fnsi.com\"}");
        sourceLoaderService.load();
        verify(syncSourceDao, times(1)).save(eq(actual1));
        verify(syncSourceDao, times(1)).save(eq(actual2));
    }

}