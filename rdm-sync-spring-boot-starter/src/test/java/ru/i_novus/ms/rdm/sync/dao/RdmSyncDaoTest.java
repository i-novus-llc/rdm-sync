package ru.i_novus.ms.rdm.sync.dao;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.test.context.jdbc.Sql;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.RdmMappingServiceImpl;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.i_novus.ms.rdm.sync.dao.RdmSyncDaoImpl.RECORD_SYS_COL;


@Sql({"/dao-test.sql"})
public class RdmSyncDaoTest extends BaseDaoTest {

    private static final String IS_DELETED_COLUMN = "deleted_ts";

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

    @Test
    public void testGetDataWithFilters() {

        final Map<String, Object> firstRow = Map.of("name", "test name1", "id", 1);
        final Map<String, Object> secondRow = Map.of("name", "test name2", "id", 2);

        String table = "ref_filtered";
        List<Map<String, Object>> rows = new ArrayList<>(List.of(firstRow, secondRow));
        rdmSyncDao.insertRows(table, rows, true);

        LocalDataCriteria criteria = createSyncedCriteria(table);
        Page<Map<String, Object>> data = rdmSyncDao.getData(criteria);
        data.getContent().forEach(map -> map.remove(IS_DELETED_COLUMN));
        assertEquals(rows, data.getContent());

        FieldValueFilter inFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, List.of(1, 2));
        FieldFilter idFilter = new FieldFilter("id", DataTypeEnum.VARCHAR, singletonList(inFilter));

        FieldValueFilter eqFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, singletonList("test name1"));
        FieldValueFilter likeFilter = new FieldValueFilter(FilterTypeEnum.LIKE, singletonList("name2"));
        FieldFilter nameFilter = new FieldFilter("name", DataTypeEnum.VARCHAR, List.of(eqFilter, likeFilter));

        LocalDataCriteria filterCriteria = createFiltersCriteria(table, List.of(idFilter, nameFilter));
        data = rdmSyncDao.getData(filterCriteria);
        data.getContent().forEach(map -> map.remove(IS_DELETED_COLUMN));
        assertEquals(rows, data.getContent());
    }

    @Test
    public void testBatchInsertAndUpdateRows() {

        List<Map<String, Object>> insertRows = new ArrayList<>();
        insertRows.add(Map.of("name", "test name1", "id", 1));
        insertRows.add(Map.of("name", "test name2", "id", 2));
        String table = "ref_ek001";

        rdmSyncDao.insertRows(table, insertRows, true);

        LocalDataCriteria criteria = createSyncedCriteria(table);
        Page<Map<String, Object>> data = rdmSyncDao.getData(criteria);
        data.getContent().forEach(map -> map.remove(IS_DELETED_COLUMN));
        assertEquals(insertRows, data.getContent());

        List<Map<String, Object>> updateRows = new ArrayList<>();
        updateRows.add(Map.of("name", "test name1 updated", "id", 1));
        updateRows.add(Map.of("name", "test name2 updated", "id", 2));

        rdmSyncDao.updateRows(table, "id", updateRows, true);
        criteria = createSyncedCriteria(table);
        data = rdmSyncDao.getData(criteria);
        data.getContent().forEach(map -> map.remove(IS_DELETED_COLUMN));
        assertEquals(updateRows, data.getContent());
    }

    @Test
    public void testInsertAndUpdateRows() {

        Map<String, Object> insertRow = Map.of("name", "test name1", "id", 1);
        String table = "ref_ek002";

        rdmSyncDao.insertRow(table, insertRow, true);

        LocalDataCriteria criteria = createSyncedCriteria(table);
        Page<Map<String, Object>> data = rdmSyncDao.getData(criteria);
        data.getContent().get(0).remove(IS_DELETED_COLUMN);
        assertEquals(insertRow, data.getContent().get(0));

        Map<String, Object> updateRow = Map.of("name", "test name1 updated", "id", 1);

        rdmSyncDao.updateRow(table, "id", updateRow, true);
        criteria = createSyncedCriteria(table);
        data = rdmSyncDao.getData(criteria);
        data.getContent().get(0).remove(IS_DELETED_COLUMN);
        assertEquals(updateRow, data.getContent().get(0));
    }

    /**
     * Создание версионной таблицы, добавление записей разных версий
     */
    @Test
    public void testVersionedTable() {

        rdmSyncDao.createVersionedTableIfNotExists(
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
        page.getContent().forEach(this::prepareRowToAssert);
        assertEquals(rows, page.getContent());

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
        firstVersionData.getContent().forEach(this::prepareRowToAssert);
        secondVersionData.getContent().forEach(this::prepareRowToAssert);
        assertEquals(rows, firstVersionData.getContent());
        secondVersionData.getContent().forEach(row -> row.values().removeAll(Collections.singleton(null)));
        assertEquals(nextVersionRows, secondVersionData.getContent());

        //просто проставление версии
    }

    private void prepareRowToAssert(Map<String, Object> row) {

        row.put("some_dt", ((Date) row.get("some_dt")).toLocalDate());
        row.remove(RECORD_SYS_COL);
    }

    @Test
    public void testAddingLoadedVersion() {

        String code = "test";
        LoadedVersion actual = new LoadedVersion(1, "test", "version",  LocalDateTime.of(2021, 11, 9, 17, 0), null);
        rdmSyncDao.insertLoadedVersion(actual.getCode(), actual.getVersion(), actual.getPublicationDate());

        LoadedVersion expected = rdmSyncDao.getLoadedVersion(code);
        Assert.assertNotNull(expected.getLastSync());
        expected.setLastSync(null);
        assertEquals(actual, expected);

        //редактируем
        actual.setVersion("2");
        rdmSyncDao.updateLoadedVersion(actual.getId(), actual.getVersion(), actual.getPublicationDate());

        expected = rdmSyncDao.getLoadedVersion(code);
        expected.setLastSync(null);
        assertEquals(actual, expected);
    }


    @Test
    public void testSaveVersionMapping() {

        String version = "CURRENT";
        String refBookCode = "test";
        String refBookName = "test Name";
        VersionMapping versionMapping = new VersionMapping(null, refBookCode, refBookName, version, "test_table","CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED);
        rdmSyncDao.insertVersionMapping(versionMapping);

        VersionMapping actual = rdmSyncDao.getVersionMapping(versionMapping.getCode(), version);
        assertEquals(versionMapping.getCode(), actual.getCode());
        assertEquals(versionMapping.getRefBookName(), actual.getRefBookName());
        assertEquals(version, actual.getVersion());
        assertMappingEquals(versionMapping, actual);

        SyncRefBook syncRefBook = rdmSyncDao.getSyncRefBook(refBookCode);
        assertEquals(new SyncRefBook(syncRefBook.getId(), refBookCode, SyncTypeEnum.NOT_VERSIONED, refBookName), syncRefBook);

        versionMapping.setDeletedField("is_deleted2");
        versionMapping.setTable("test_table2");
        versionMapping.setMappingVersion(1);
        rdmSyncDao.updateCurrentMapping(versionMapping);

        actual = rdmSyncDao.getVersionMapping(versionMapping.getCode(), version);
        assertEquals(version, versionMapping.getVersion());
        assertMappingEquals(versionMapping, actual);
    }

    @Test
    public void testInternalLocalRowStateUpdateTriggerListIsEmpty(){

        rdmSyncDao.createVersionedTableIfNotExists(
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
        boolean actual = rdmSyncDao.existsInternalLocalRowStateUpdateTrigger("public.ref_ek001_ver");
        assertFalse(actual);
    }

    @Test
    public void testGetLastMappingVersion() {

        Assert.assertEquals(-1, rdmSyncDao.getLastMappingVersion("testCode"));
    }

    @Test
    public void testMarkDeletedToMultipleRows(){
        LocalDateTime expectedNowDeletedDate = LocalDateTime.of(2021, 9, 26, 12, 30);
        LocalDateTime expectedBeforeDeletedDate = LocalDateTime.of(2021, 9, 25, 12, 30);
        rdmSyncDao.markDeleted("ref_cars", "deleted_ts", expectedNowDeletedDate, true );
        List<Map<String, Object>> content = rdmSyncDao.getData(new LocalDataCriteria("ref_cars", "id", 10, 0, RdmSyncLocalRowState.SYNCED, null, null)).getContent();
        LocalDateTime actualBeforeDeleted = (LocalDateTime) content.stream().filter(row -> row.get("id").equals(1)).findAny().get().get("deleted_ts");
        LocalDateTime actualNowDeleted = (LocalDateTime) content.stream().filter(row -> row.get("id").equals(2)).findAny().get().get("deleted_ts");
        Assert.assertEquals(expectedBeforeDeletedDate,  actualBeforeDeleted);
        Assert.assertEquals(expectedNowDeletedDate,  actualNowDeleted);
    }

    private void assertMappingEquals(VersionMapping expected, VersionMapping actual) {
        Assert.assertEquals(expected.getMappingVersion(), actual.getMappingVersion());
        Assert.assertEquals(expected.getDeletedField(), actual.getDeletedField());
        Assert.assertEquals(expected.getPrimaryField(), actual.getPrimaryField());
        Assert.assertEquals(expected.getTable(), actual.getTable());
        Assert.assertEquals(expected.getType(), actual.getType());
    }

    private LocalDataCriteria createSyncedCriteria(String table) {

        return new LocalDataCriteria(table,
                "id", 10, 0, null,
                RdmSyncLocalRowState.SYNCED, null);
    }

    private LocalDataCriteria createFiltersCriteria(String table, List<FieldFilter> filters) {

        return new LocalDataCriteria(table,
                "id", 10, 0, filters,
                RdmSyncLocalRowState.SYNCED, null);
    }
}

