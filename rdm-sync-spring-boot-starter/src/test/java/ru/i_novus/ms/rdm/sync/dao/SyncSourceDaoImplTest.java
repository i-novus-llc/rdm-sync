package ru.i_novus.ms.rdm.sync.dao;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;

@Ignore
class SyncSourceDaoImplTest extends BaseDaoTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Configuration
    static class Config {

        @Bean
        public SyncSourceDaoImpl syncSourceDao() {
            return new SyncSourceDaoImpl();
        }
    }

    @Autowired
    SyncSourceDaoImpl syncSourceDao;

    @Test
    void testSave() {

        SyncSource actualSyncSource = new SyncSource("name", "CODE-1", "{}", "service");
        syncSourceDao.save(actualSyncSource);
        SyncSource expectedSyncSource = syncSourceDao.findByCode("CODE-1");
        Assertions.assertEquals(expectedSyncSource,actualSyncSource);

        SyncSource actualSyncSourceOnConflict = new SyncSource("new-name", "CODE-1", "{new json}", "anotherService");
        syncSourceDao.save(actualSyncSourceOnConflict);
        SyncSource expectedSyncSourceOnConflict = syncSourceDao.findByCode("CODE-1");
        Assertions.assertEquals(expectedSyncSourceOnConflict,actualSyncSourceOnConflict);

    }
}