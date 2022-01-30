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

import javax.ws.rs.BadRequestException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

@Sql({"/dao-test.sql"})
public class RdmSyncDaoTest extends BaseDaoTest {

    private static final String RECORD_SYS_COL = "_sync_rec_id";

    private static final String DELETED_FIELD_COL = "deleted_ts";

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
        final Map<String, Object> thridRow = Map.of("name", "test\" name'3", "id", 3);

        String table = "ref_filtered";
        List<Map<String, Object>> rows = new ArrayList<>(List.of(firstRow, secondRow));
        rdmSyncDao.insertRows(table, rows, true);

        // Проверка отсутствия фильтрации.
        LocalDataCriteria criteria = createSyncedCriteria(table);
        Page<Map<String, Object>> data = rdmSyncDao.getData(criteria);
        data.getContent().forEach(this::removeSystemColumns);
        assertEquals(rows, data.getContent());

        // Проверка наличия фильтрации.
        FieldValueFilter inFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, List.of(1, 2, 3));
        FieldFilter idFilter = new FieldFilter("id", DataTypeEnum.VARCHAR, singletonList(inFilter));

        FieldValueFilter eqFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, singletonList("test name1"));

        FieldValueFilter likeFilter = new FieldValueFilter(FilterTypeEnum.LIKE, singletonList("name2"));
        FieldValueFilter ilikeFilter = new FieldValueFilter(FilterTypeEnum.ILIKE, singletonList("Name2"));

        FieldValueFilter qlikeFilter = new FieldValueFilter(FilterTypeEnum.QLIKE, singletonList("name3"));
        FieldValueFilter iqlikeFilter = new FieldValueFilter(FilterTypeEnum.IQLIKE, singletonList("Test Name3"));

        FieldValueFilter isNullFilter = new FieldValueFilter(FilterTypeEnum.IS_NULL, singletonList(null));
        FieldValueFilter isNotNullFilter = new FieldValueFilter(FilterTypeEnum.IS_NOT_NULL, singletonList(null));

        FieldFilter nameFilter = new FieldFilter("name", DataTypeEnum.VARCHAR,
                List.of(eqFilter,
                        likeFilter, ilikeFilter,
                        qlikeFilter, iqlikeFilter,
                        isNullFilter, isNotNullFilter));

        LocalDataCriteria filterCriteria = createFiltersCriteria(table, List.of(idFilter, nameFilter));
        data = rdmSyncDao.getData(filterCriteria);
        data.getContent().forEach(this::removeSystemColumns);
        assertEquals(rows, data.getContent());

        // Проверка поиска по systemId.
        // -- Получение записи с systemId по первичному ключу.
        FieldValueFilter pkValueFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, List.of(2));
        FieldFilter pkFilter = new FieldFilter("id", DataTypeEnum.VARCHAR, singletonList(pkValueFilter));
        filterCriteria = createFiltersCriteria(table, singletonList(pkFilter));
        data = rdmSyncDao.getData(filterCriteria);
        assertEquals(1, data.getContent().size());

        Map<String, Object> row = data.getContent().get(0);
        Long systemId = (Long) row.get(RECORD_SYS_COL);
        assertNotNull(systemId);

        // -- Получение той же записи по systemId.
        LocalDataCriteria systemIdCriteria = createSyncedCriteria(table);
        systemIdCriteria.setRecordId(systemId);
        systemIdCriteria.setSysPkColumn("_sync_rec_id");

        data = rdmSyncDao.getData(systemIdCriteria);
        assertEquals(1, data.getContent().size());
        assertEquals(row, data.getContent().get(0));
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
        data.getContent().forEach(this::removeSystemColumns);
        assertEquals(insertRows, data.getContent());

        List<Map<String, Object>> updateRows = new ArrayList<>();
        updateRows.add(Map.of("name", "test name1 updated", "id", 1));
        updateRows.add(Map.of("name", "test name2 updated", "id", 2));

        rdmSyncDao.updateRows(table, "id", updateRows, true);
        criteria = createSyncedCriteria(table);
        data = rdmSyncDao.getData(criteria);
        data.getContent().forEach(this::removeSystemColumns);
        assertEquals(updateRows, data.getContent());
    }

    @Test
    public void testInsertAndUpdateRows() {

        Map<String, Object> insertRow = Map.of("name", "test name1", "id", 1);
        String table = "ref_ek002";

        rdmSyncDao.insertRow(table, insertRow, true);

        LocalDataCriteria criteria = createSyncedCriteria(table);
        Page<Map<String, Object>> data = rdmSyncDao.getData(criteria);

        Map<String, Object> row = data.getContent().get(0);
        removeSystemColumns(row);
        assertEquals(insertRow, row);

        Map<String, Object> updateRow = Map.of("name", "test name1 updated", "id", 1);

        rdmSyncDao.updateRow(table, "id", updateRow, true);
        criteria = createSyncedCriteria(table);
        data = rdmSyncDao.getData(criteria);

        row = data.getContent().get(0);
        removeSystemColumns(row);
        assertEquals(updateRow, row);
    }

    /**
     * Создание версионной таблицы, добавление записей разных версий
     */
    @Test
    public void testVersionedTable() {

        rdmSyncDao.createVersionedTableIfNotExists(
                "public",
                "ref_ek001_ver",
                generateFieldMappings(),
                RECORD_SYS_COL);

        List<Map<String, Object>> rows = List.of(
                Map.of("ID", 1, "name", "name1", "some_dt", LocalDate.of(2021, 1, 1), "flag", true),
                Map.of("ID", 2, "name", "name2", "some_dt", LocalDate.of(2021, 1, 2), "flag", false)
        );
        rdmSyncDao.insertVersionedRows("public.ref_ek001_ver", rows, "1.0");
        Page<Map<String, Object>> page = rdmSyncDao.getVersionedData(new VersionedLocalDataCriteria("ek001", "public.ref_ek001_ver", "ID", 30, 0, null, null));
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

        Page<Map<String, Object>> firstVersionData = rdmSyncDao.getVersionedData(new VersionedLocalDataCriteria("ek001", "public.ref_ek001_ver", "ID", 30, 0, null, "1.0"));
        Page<Map<String, Object>> secondVersionData = rdmSyncDao.getVersionedData(new VersionedLocalDataCriteria("ek001", "public.ref_ek001_ver", "ID", 30, 0, null, "2.0"));
        firstVersionData.getContent().forEach(this::prepareRowToAssert);
        secondVersionData.getContent().forEach(this::prepareRowToAssert);
        assertEquals(rows, firstVersionData.getContent());
        secondVersionData.getContent().forEach(row -> row.values().removeAll(Collections.singleton(null)));
        assertEquals(nextVersionRows, secondVersionData.getContent());

        //просто проставление версии
    }

    @Test
    public void testTableWithNaturalPrimaryKey() {
        String schema = "public";
        String table = "ref_001_with_natural_pk";
        String sysPkColumn = "id";


        List<FieldMapping> fields = List.of(
                new FieldMapping("id", "integer", "id"),
                new FieldMapping("name", "varchar", "name"),
                new FieldMapping("age", "integer", "age")
        );

        rdmSyncDao.createTableWithNaturalPrimaryKeyIfNotExists(
                schema,
                table,
                fields,
                DELETED_FIELD_COL,
                sysPkColumn
        );

        rdmSyncDao.addInternalLocalRowStateColumnIfNotExists(schema, table);

        List<Map<String, Object>> insertRows = List.of(
                new HashMap<>(Map.of( "id", 1, "name", "name1", "age", 20)),
                new HashMap<>(Map.of( "id", 2, "name", "name2", "age", 25)),
                new HashMap<>(Map.of( "id", 3, "name", "name3", "age", 30))
        );

        rdmSyncDao.insertRows(
                String.format("%s.%s", schema, table),
                insertRows,
                true
        );

        LocalDataCriteria criteria = new LocalDataCriteria(String.format("%s.%s", schema, table), sysPkColumn, 10, 0, null);
        Page<Map<String, Object>> data = rdmSyncDao.getData(criteria);

        List<Map<String, Object>> rows = data.getContent();
        rows.forEach(r -> r.remove(DELETED_FIELD_COL));

        assertEquals(insertRows, data.getContent());

        insertRows.get(0).put("name", "name4");
        rdmSyncDao.updateRows(
                String.format("%s.%s", schema, table),
                sysPkColumn,
                insertRows,
                true);

        criteria = new LocalDataCriteria(String.format("%s.%s", schema, table), sysPkColumn, 10, 0, null);

        data = rdmSyncDao.getData(criteria);

        rows = data.getContent();
        rows.forEach(r -> r.remove(DELETED_FIELD_COL));

        assertEquals(insertRows, rows);

    }

    private void prepareRowToAssert(Map<String, Object> row) {

        row.put("some_dt", ((Date) row.get("some_dt")).toLocalDate());
        row.remove(RECORD_SYS_COL);
    }

    @Test
    public void testAddingLoadedVersion() {

        String code = "testAddingLoadedVersion";
        String version ="version";
        LoadedVersion expected = new LoadedVersion(null, code, version,  LocalDateTime.of(2021, 11, 9, 17, 0), null, null, true);
        Integer loadedVerId = rdmSyncDao.insertLoadedVersion(expected.getCode(), expected.getVersion(), expected.getPublicationDate(), expected.getCloseDate(), expected.getActual());

        LoadedVersion actual = rdmSyncDao.getLoadedVersion(code, version);
        assertNotNull(actual.getLastSync());
        actual.setLastSync(null);
        expected.setId(loadedVerId);
        assertEquals(expected, actual);

        //редактируем
        expected.setVersion("2");
        rdmSyncDao.updateLoadedVersion(expected.getId(), expected.getVersion(), expected.getPublicationDate(), expected.getCloseDate());

        actual = rdmSyncDao.getLoadedVersion(code, expected.getVersion());
        actual.setLastSync(null);
        assertEquals(expected, actual);
    }


    @Test
    public void testSaveVersionMapping() {

        String version = "CURRENT";
        String refBookCode = "test";
        String refBookName = "test Name";
        String pkSysColumn = "test_pk_field";
        VersionMapping versionMapping = new VersionMapping(null, refBookCode, refBookName, version, "test_table", pkSysColumn,"CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED, null);
        rdmSyncDao.insertVersionMapping(versionMapping);

        VersionMapping actual = rdmSyncDao.getVersionMapping(versionMapping.getCode(), version);
        assertEquals(versionMapping.getCode(), actual.getCode());
        assertEquals(versionMapping.getRefBookName(), actual.getRefBookName());
        assertEquals(version, actual.getVersion());
        assertMappingEquals(versionMapping, actual);

        SyncRefBook syncRefBook = rdmSyncDao.getSyncRefBook(refBookCode);
        assertEquals(new SyncRefBook(syncRefBook.getId(), refBookCode, SyncTypeEnum.NOT_VERSIONED, refBookName, null), syncRefBook);

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
                generateFieldMappings(),
                "sys_pk");

        List<Map<String, Object>> rows = generateRows();
        rdmSyncDao.insertVersionedRows("public.ref_ek001_ver", rows, "1.0");
        boolean actual = rdmSyncDao.existsInternalLocalRowStateUpdateTrigger("public.ref_ek001_ver");
        assertFalse(actual);
    }

    private List<Map<String, Object>> generateRows() {
        return List.of(
                Map.of("ID", 1, "name", "name1", "some_dt", LocalDate.of(2021, 1, 1), "flag", true),
                Map.of("ID", 2, "name", "name2", "some_dt", LocalDate.of(2021, 1, 2), "flag", false)
        );
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

        LocalDataCriteria criteria = createSyncedCriteria("ref_cars");
        List<Map<String, Object>> content = rdmSyncDao.getData(criteria).getContent();

        LocalDateTime actualBeforeDeleted = (LocalDateTime) content.stream().filter(row -> row.get("id").equals(1)).findAny().get().get("deleted_ts");
        LocalDateTime actualNowDeleted = (LocalDateTime) content.stream().filter(row -> row.get("id").equals(2)).findAny().get().get("deleted_ts");
        Assert.assertEquals(expectedBeforeDeletedDate,  actualBeforeDeleted);
        Assert.assertEquals(expectedNowDeletedDate,  actualNowDeleted);
    }

    @Test
    public void testCRUSimpleVersionedData() {
        List<FieldMapping> fieldMappings = generateFieldMappings();
        LocalDateTime publishDate = LocalDateTime.of(2022, 1, 1, 12, 0);
        rdmSyncDao.createSimpleVersionedTables("public", "simple_ver_table", fieldMappings);
        Integer loadedVersionId = rdmSyncDao.insertLoadedVersion("test", "1.0", publishDate, null, true);
        List<Map<String, Object>> rows = generateRows();
        rdmSyncDao.insertSimpleVersionedRows("public.simple_ver_table", rows, loadedVersionId);
        VersionedLocalDataCriteria criteria = new VersionedLocalDataCriteria("test", "public.simple_ver_table", "_sync_rec_id", 100, 0, null, "1.0");
        Page<Map<String, Object>> simpleVersionedData = rdmSyncDao.getSimpleVersionedData(criteria);
        simpleVersionedData.getContent().forEach(this::prepareRowToAssert);
        Assert.assertEquals(rows, simpleVersionedData.getContent());


        //грузим след версию
        LocalDateTime secondVersionPublishDate = LocalDateTime.of(2022, 2, 2, 10, 0);
        rdmSyncDao.closeLoadedVersion("test", "1.0", secondVersionPublishDate);
        Integer secondLoadedVersionId = rdmSyncDao.insertLoadedVersion("test", "1.1", secondVersionPublishDate, null, true);
        List<Map<String, Object>> secondVersionRows = new ArrayList<>(rows);
        secondVersionRows.add(Map.of("ID", 3, "name", "name3", "some_dt", LocalDate.of(2021, 1, 3), "flag", false));
        rdmSyncDao.insertSimpleVersionedRows("public.simple_ver_table", secondVersionRows, secondLoadedVersionId);
        //получаем версию 1.1
        Page<Map<String, Object>> secondVersionData = rdmSyncDao.getSimpleVersionedData(new VersionedLocalDataCriteria("test", "public.simple_ver_table", "_sync_rec_id", 100, 0, null, "1.1"));
        secondVersionData.getContent().forEach(this::prepareRowToAssert);
        Assert.assertEquals(secondVersionRows, secondVersionData.getContent());
        Assert.assertEquals(secondVersionPublishDate, rdmSyncDao.getLoadedVersion("test", "1.0").getCloseDate());
    }

    @Test(expected = BadRequestException.class)
    public void testGetSimpleVersionedDataOnRefbooksWithSameVersionNumber() {
        LocalDateTime publishDate = LocalDateTime.of(2022, 1, 1, 12, 0);
        rdmSyncDao.createSimpleVersionedTables("public", "simple_ver_table", generateFieldMappings());
        rdmSyncDao.insertLoadedVersion("test", "1.0", publishDate, null, true);
        rdmSyncDao.insertLoadedVersion("another_test_refbook_with_same_loaded_version", "1.0", publishDate, null, true);
        VersionedLocalDataCriteria criteria = new VersionedLocalDataCriteria("test", "public.simple_ver_table", "_sync_rec_id", 100, 0, null, "1.0");
        Page<Map<String, Object>> simpleVersionedData = rdmSyncDao.getSimpleVersionedData(criteria);
        Assert.assertEquals(0, simpleVersionedData.getTotalElements());

        criteria.setRefBookCode(null);
        rdmSyncDao.getSimpleVersionedData(criteria);
    }

    private List<FieldMapping> generateFieldMappings() {
        return List.of(
                new FieldMapping("ID", "integer", "ID"),
                new FieldMapping("name", "varchar", "NAME"),
                new FieldMapping("some_dt", "date", "DT"),
                new FieldMapping("flag", "boolean", "FLAG"));
    }

    private void assertMappingEquals(VersionMapping expected, VersionMapping actual) {
        Assert.assertEquals(expected.getMappingVersion(), actual.getMappingVersion());
        Assert.assertEquals(expected.getDeletedField(), actual.getDeletedField());
        Assert.assertEquals(expected.getPrimaryField(), actual.getPrimaryField());
        Assert.assertEquals(expected.getTable(), actual.getTable());
        Assert.assertEquals(expected.getType(), actual.getType());
    }

    private LocalDataCriteria createSyncedCriteria(String table) {

        return new LocalDataCriteria(table, "id", 10, 0, null);
    }

    private LocalDataCriteria createFiltersCriteria(String table, List<FieldFilter> filters) {

        return new LocalDataCriteria(table, "id", 10, 0, filters);
    }

    private void removeSystemColumns(Map<String, Object> row) {

        row.remove(RECORD_SYS_COL);
        row.remove(DELETED_FIELD_COL);
    }
}

