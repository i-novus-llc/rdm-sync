package ru.i_novus.ms.rdm.sync.impl;

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
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RdmSourceProperty.class)
@EnableConfigurationProperties(RdmSourceProperty.class)
@TestPropertySource("classpath:fnsi-source-test.properties")
public class RdmSourceLoaderServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Autowired
    RdmSourceProperty rdmSourceProperty;

    @Mock
    private SyncSourceDao syncSourceDao;

    private RdmSourceLoaderService sourceLoaderService;

    @Before
    public void setUp() throws Exception {
        sourceLoaderService = new RdmSourceLoaderService(rdmSourceProperty, syncSourceDao);
    }

    @Test
    public void testLoad() {

    }
}