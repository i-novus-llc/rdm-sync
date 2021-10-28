package ru.i_novus.ms.rdm.sync.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.RdmMappingServiceImpl;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@JdbcTest
@Sql({"/dao-test.sql"})
public class RdmSyncDaoTest {

    private static final String IS_DELETED_COLUMN = "is_deleted";

    @Configuration
    static class Config {
        @Bean
        public RdmSyncDao rdmSyncDao() {
            return new RdmSyncDaoImpl();
        }

        @Bean
        public RdmMappingService rdmMappingService(){
            return new RdmMappingServiceImpl();
        }
    }

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testBatchInsertAndUpdate() {
        List<Map<String, Object>> insertRows = new ArrayList<>();
        insertRows.add(Map.of("name", "test name1", "id", 1));
        insertRows.add(Map.of("name", "test name2", "id", 2));
        String table = "ref_ek001";

        rdmSyncDao.insertRows(table, insertRows, true);

        Page<Map<String, Object>> data = rdmSyncDao.getData(new LocalDataCriteria(table, "id", 10, 0, RdmSyncLocalRowState.SYNCED, null, null));
        data.getContent().forEach(map -> map.remove(IS_DELETED_COLUMN));
        Assert.assertEquals(insertRows, data.getContent());

        List<Map<String, Object>> updateRows = new ArrayList<>();
        updateRows.add(Map.of("name", "test name1 updated", "id", 1));
        updateRows.add(Map.of("name", "test name2 updated", "id", 2));

        rdmSyncDao.updateRows(table, "id", updateRows, true);
        data = rdmSyncDao.getData(new LocalDataCriteria(table, "id", 10, 0, RdmSyncLocalRowState.SYNCED, null, null));
        data.getContent().forEach(map -> map.remove(IS_DELETED_COLUMN));
        Assert.assertEquals(updateRows, data.getContent());

    }

    @Test
    public void testInsertAndUpdate() {

        Map<String, Object> insertRow = Map.of("name", "test name1", "id", 1);
        String table = "ref_ek002";

        rdmSyncDao.insertRow(table, insertRow, true);

        Page<Map<String, Object>> data = rdmSyncDao.getData(new LocalDataCriteria(table, "id", 10, 0, RdmSyncLocalRowState.SYNCED, null, null));
        data.getContent().get(0).remove(IS_DELETED_COLUMN);
        Assert.assertEquals(insertRow, data.getContent().get(0));

        Map<String, Object> updateRow = Map.of("name", "test name1 updated", "id", 1);

        rdmSyncDao.updateRow(table, "id", updateRow, true);
        data = rdmSyncDao.getData(new LocalDataCriteria(table, "id", 10, 0, RdmSyncLocalRowState.SYNCED, null, null));
        data.getContent().get(0).remove(IS_DELETED_COLUMN);
        Assert.assertEquals(updateRow, data.getContent().get(0));

    }
}
