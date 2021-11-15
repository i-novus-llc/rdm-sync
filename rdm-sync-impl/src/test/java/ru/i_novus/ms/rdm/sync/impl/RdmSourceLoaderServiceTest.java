package ru.i_novus.ms.rdm.sync.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:rdm-source-test.properties")
public class RdmSourceLoaderServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();


    @Mock
    private SyncSourceDao syncSourceDao;

    @Value("${rdm.backend.path}")
    private String url;

    private RdmSourceLoaderService sourceLoaderService;

    @Before
    public void setUp() throws Exception {
        sourceLoaderService = new RdmSourceLoaderService(url, syncSourceDao);
    }

    @Test
    public void testLoad() {
        SyncSource syncSourceActual = new SyncSource("", "", "abc", "RdmSourceLoaderService");
        sourceLoaderService.load();
        verify(syncSourceDao).save(eq(syncSourceActual));
    }
}