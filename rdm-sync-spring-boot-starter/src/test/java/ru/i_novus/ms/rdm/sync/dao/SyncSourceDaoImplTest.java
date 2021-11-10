package ru.i_novus.ms.rdm.sync.dao;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;

@JdbcTest
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "/application-test.properties")
class SyncSourceDaoImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Configuration
    static class Config {
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void testSave() {
        SyncSourceDaoImpl syncSourceDao = new SyncSourceDaoImpl(jdbcTemplate);
        SyncSource syncSource = new SyncSource("", "", "{}");
        syncSourceDao.save(syncSource);

    }
}