package ru.i_novus.ms.rdm.sync.init.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.init.dao.pg.impl.SyncSourceSavingDaoImpl;

class SyncSourceDaoImplTest extends BaseDaoTest {

    @Configuration
    static class Config {

        @Bean
        public SyncSourceSavingDaoImpl syncSourceDao() {
            return new SyncSourceSavingDaoImpl();
        }
    }

    @Autowired
    SyncSourceSavingDaoImpl syncSourceDao;

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