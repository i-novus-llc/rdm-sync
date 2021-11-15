package ru.i_novus.ms.rdm.sync.dao;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.RdmMappingServiceImpl;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Sql({"/dao-test.sql"})
public class RdmSyncDaoTest extends BaseDaoTest {

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

        @Bean
        public SyncSourceDao syncSourceDao() {return new SyncSourceDaoImpl();}
    }

    @Autowired
    private SyncSourceDao sourceDao;

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

    @Test
    /**
     * Создание версионной таблицы, добавление записей разных версий
     */
    public void testVersionedTable() {
        rdmSyncDao.createVersionedTable(
                "public",
                "ref_ek001_ver",
                List.of(
                        new FieldMapping("ID", "integer", "ID"),
                        new FieldMapping("name", "varchar", "NAME"),
                        new FieldMapping("some_dt", "date", "DT"),
                        new FieldMapping("flag", "boolean", "FLAG")));

        List<Map<String, Object>> rows = List.of(
                Map.of("ID", 1, "name", "name1", "some_dt", LocalDate.of(2021, 1, 1), "flag", true),
                Map.of("ID", 2, "name", "name2", "some_dt", LocalDate.of(2021, 1, 2), "flag", false)
        );
        rdmSyncDao.insertVersionedRows("public.ref_ek001_ver", rows, "1.0");
        Page<Map<String, Object>> page = rdmSyncDao.getVersionedData(new VersionedLocalDataCriteria("public.ref_ek001_ver", "ID", 30, 0, null, null));
        page.getContent().forEach(row -> row.put("some_dt", ((Date) row.get("some_dt")).toLocalDate()));
        Assert.assertEquals(rows, page.getContent());

        // добавление другой версии(upsert), поиск по версии
        List<Map<String, Object>> nextVersionRows = List.of(
                //строка не изменилась
                Map.of("ID", 1, "name", "name1", "some_dt", LocalDate.of(2021, 1, 1), "flag", true),
                //изменилась
                Map.of("ID", 2, "name", "name2 edited", "some_dt", LocalDate.of(2021, 1, 2), "flag", false),
                //новая
                Map.of("ID", 3, "name", "name3", "some_dt", LocalDate.of(2021, 1, 3))
        );
        rdmSyncDao.upsertVersionedRows("public.ref_ek001_ver", nextVersionRows, "2.0");

        Page<Map<String, Object>> firstVersionData = rdmSyncDao.getVersionedData(new VersionedLocalDataCriteria("public.ref_ek001_ver", "ID", 30, 0, null, "1.0"));
        Page<Map<String, Object>> secondVersionData = rdmSyncDao.getVersionedData(new VersionedLocalDataCriteria("public.ref_ek001_ver", "ID", 30, 0, null, "2.0"));
        firstVersionData.getContent().forEach(row -> row.put("some_dt", ((Date) row.get("some_dt")).toLocalDate()));
        secondVersionData.getContent().forEach(row -> row.put("some_dt", ((Date) row.get("some_dt")).toLocalDate()));
        Assert.assertEquals(rows, firstVersionData.getContent());
        secondVersionData.getContent().forEach(row -> row.values().removeAll(Collections.singleton(null)));
        Assert.assertEquals(nextVersionRows, secondVersionData.getContent());

        //просто проставление версии
    }

    @Test
    public void testAddingLoadedVersion() {
        String code = "test";
        LoadedVersion actual = new LoadedVersion(1, "test", "version",  LocalDateTime.of(2021, 11, 9, 17, 0), null);
        rdmSyncDao.insertLoadedVersion(actual.getCode(), actual.getVersion(), actual.getPublicationDate());
        LoadedVersion expected = rdmSyncDao.getLoadedVersion(code);
        Assert.assertNotNull(expected.getLastSync());
        expected.setLastSync(null);
        Assert.assertEquals(actual, expected);
        //редактируем
        actual.setVersion("2");
        rdmSyncDao.updateLoadedVersion(actual.getId(), actual.getVersion(), actual.getPublicationDate());
        expected = rdmSyncDao.getLoadedVersion(code);
        expected.setLastSync(null);
        Assert.assertEquals(actual, expected);
    }


    @Test
    public void testSaveVersionMappingFromXml() {
        SyncSource actualSyncSource1 = new SyncSource("name", "CODE-1", "{}");
        sourceDao.save(actualSyncSource1);

        XmlMappingRefBook xmlMappingRefBook = new XmlMappingRefBook();
        xmlMappingRefBook.setCode("test");
        xmlMappingRefBook.setDeletedField("is_deleted");
        xmlMappingRefBook.setUniqueSysField("id");
        xmlMappingRefBook.setSysTable("test_table");
        xmlMappingRefBook.setMappingVersion(-1);
        xmlMappingRefBook.setSource("CODE-1");

        SyncSource actualSyncSource2 = new SyncSource("name", "CODE-2", "{}");
        sourceDao.save(actualSyncSource2);


        rdmSyncDao.insertVersionMapping(xmlMappingRefBook);
        VersionMapping versionMapping = rdmSyncDao.getVersionMapping(xmlMappingRefBook.getCode(), "CURRENT");
        Assert.assertEquals(xmlMappingRefBook.getCode(), versionMapping.getCode());
        Assert.assertEquals("CURRENT", versionMapping.getVersion());
        assertEquals(xmlMappingRefBook, versionMapping);

        xmlMappingRefBook.setDeletedField("is_deleted2");
        xmlMappingRefBook.setSysTable("test_table2");
        xmlMappingRefBook.setMappingVersion(1);
        xmlMappingRefBook.setSource("CODE-2");
        rdmSyncDao.updateVersionMapping(xmlMappingRefBook);
        versionMapping = rdmSyncDao.getVersionMapping(xmlMappingRefBook.getCode(), "CURRENT");
        Assert.assertEquals("CURRENT", versionMapping.getVersion());
        assertEquals(xmlMappingRefBook, versionMapping);

    }

    private void assertEquals(XmlMappingRefBook xmlMappingRefBook, VersionMapping versionMapping) {
        Assert.assertEquals(xmlMappingRefBook.getMappingVersion(), versionMapping.getMappingVersion());
        Assert.assertEquals(xmlMappingRefBook.getDeletedField(), versionMapping.getDeletedField());
        Assert.assertEquals(xmlMappingRefBook.getUniqueSysField(), versionMapping.getPrimaryField());
        Assert.assertEquals(xmlMappingRefBook.getSysTable(), versionMapping.getTable());
        Assert.assertEquals(xmlMappingRefBook.getSource(), versionMapping.getSource());
    }
}

