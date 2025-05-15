package ru.i_novus.ms.rdm.sync.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.Assert;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.api.model.PgTable;
import ru.i_novus.ms.rdm.sync.init.dao.pg.impl.VersionedLocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JdbcTest(properties = {
        "spring.liquibase.enabled=false"
})
@Sql(scripts = {"/versionedDataDaoTest.sql"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VersionedDataDaoTest extends BaseDaoTest {

    private static final String RECORD_FROM_DT = "_sync_from_dt";
    private static final String RECORD_TO_DT = "_sync_to_dt";

    //даты версий
    private static final Integer firstVersion = 1;
    private static final Integer nextVersion = 2;
    private static final Integer nextVersion2 = 3;

    private static final LocalDate secondVersionCreatedDt = LocalDate.now();
    private static final LocalDate firstVersionCreatedDt = secondVersionCreatedDt.minusDays(3);

    private static final String tempVersionedRef3 = "temp_versioned_ref3";
    private static List<Map<String, Object>> version3;

    private static final String refbookCode = "test";
    private static final String refTable = "versioned_ref";
    private static final String sysPkColumn = "_sync_rec_id";

    @Configuration
    static class Config {

        @Bean
        RdmSyncDao rdmSyncDao() {
            return new RdmSyncDaoImpl();
        }

        @Bean
        VersionedDataDao versionedDataDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate, RdmSyncDao rdmSyncDao) {
            return new VersionedDataDaoImpl(namedParameterJdbcTemplate, rdmSyncDao);
        }

        @Bean
        VersionedLocalRefBookCreatorDao versionedLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
            return new VersionedLocalRefBookCreatorDao(namedParameterJdbcTemplate);
        }


    }

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Autowired
    private VersionedDataDao versionedDataDao;

    @Autowired
    private VersionedLocalRefBookCreatorDao versionedLocalRefBookCreatorDao;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Тестирует CRUD операции версионных данных
     */
    @Test
    @Order(1)
    void testCRUDVersionedData() {

        VersionMapping versionMapping = VersionMapping.builder()
                .primaryField("num")
                .table(refTable)
                .sysPkColumn(sysPkColumn)
                .build();

        List<FieldMapping> fieldMappings = List.of(
                new FieldMapping("name", "varchar", "name"),
                new FieldMapping("num", "integer", "num"),
                new FieldMapping("flag", "boolean", "num"),
                new FieldMapping("some_dt", "date", "some_dt")
        );

        PgTable pgTable = new PgTable(versionMapping, fieldMappings, null, Map.of());

        versionedLocalRefBookCreatorDao.createTable(
                refTable,
                refbookCode,
                versionMapping,
                fieldMappings,
                "some table",
                Map.of()
        );

        loadFirstVersion(pgTable);

        //загружаем следующую версию
        loadSecondVersion(pgTable);

        //следующая версия
        loadThirdVersion(pgTable);

        //repeatVersion
        repeatVersion(pgTable);
    }

    private void loadFirstVersion(PgTable pgTable) {
        String tempVersionedRef = "temp_versioned_ref";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef,
                pgTable.getName().replace("\"", ""),
                sysPkColumn,
                "num");

        List<Map<String, Object>> version1 = List.of(
                Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firstVersionCreatedDt),
                Map.of("name", "name2", "num", 2, "flag", false, "some_dt", firstVersionCreatedDt),
                Map.of("name", "name10", "num", 10, "flag", false, "some_dt", firstVersionCreatedDt)
        );

        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef,
                version1
        );

        //загружаем первую версию
        versionedDataDao.addFirstVersionData(tempVersionedRef, pgTable, firstVersion);

        //получаем данные справочника первой версии
        VersionedLocalDataCriteria localDataCriteria = new VersionedLocalDataCriteria(refbookCode, pgTable.getName().replace("\"", ""), "num", 100, 0, new ArrayList<>(), firstVersion.toString());
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(3, data.getTotalElements());
        compare(data.getContent(), version1, localDataCriteria.getPk());

        // фильтруем по дате вне версии
        localDataCriteria.setVersion("0");
        data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(0, data.getTotalElements());
    }

    private void loadSecondVersion(PgTable pgTable) {
        String tempVersionedRef2 = "temp_versioned_ref2";
        rdmSyncDao.createDiffTempDataTbl(
                tempVersionedRef2,
                pgTable.getName().replace("\"", ""));

        List<Map<String, Object>> version2 = List.of(
                Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firstVersionCreatedDt),
                Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        List<Map<String, Object>> version2ForDelete = List.of(
                Map.of("name", "name10", "num", 10, "flag", false, "some_dt", firstVersionCreatedDt)
        );

        List<Map<String, Object>> version2ForUpdate = List.of(
                Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        List<Map<String, Object>> version2ForInsert = List.of(
                Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        rdmSyncDao.insertDiffAsTempData(
                tempVersionedRef2,
                version2ForInsert,
                version2ForUpdate,
                version2ForDelete
        );

        versionedDataDao.addDiffVersionData(tempVersionedRef2, pgTable, refbookCode, nextVersion, firstVersion.toString());

        //проверка
        VersionedLocalDataCriteria localDataCriteria = new VersionedLocalDataCriteria(refbookCode, pgTable.getName().replace("\"", ""), "num", 100, 0, new ArrayList<>(), nextVersion.toString());
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(5, data.getTotalElements());
        compare(data.getContent(), version2, localDataCriteria.getPk());
    }

    private void loadThirdVersion(PgTable pgTable) {

        rdmSyncDao.createDiffTempDataTbl(
                tempVersionedRef3,
                pgTable.getName().replace("\"", ""));

        version3 = List.of(
                Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firstVersionCreatedDt),
                Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name6", "num", 6, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name7", "num", 7, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name8", "num", 8, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name9", "num", 9, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        List<Map<String, Object>> version3ForInsert = List.of(
                Map.of("name", "name6", "num", 6, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name7", "num", 7, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name8", "num", 8, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name9", "num", 9, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        rdmSyncDao.insertDiffAsTempData(
                tempVersionedRef3,
                version3ForInsert,
                List.of(),
                List.of()
        );

        versionedDataDao.addDiffVersionData(tempVersionedRef3, pgTable, refbookCode, nextVersion2, nextVersion.toString());

        //проверка
        VersionedLocalDataCriteria localDataCriteria = new VersionedLocalDataCriteria(refbookCode, pgTable.getName().replace("\"", ""), "num", 100, 0, new ArrayList<>(), nextVersion2.toString());
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(9, data.getTotalElements());
        compare(data.getContent(), version3, localDataCriteria.getPk());
    }

    private void repeatVersion(PgTable pgTable) {

        String tempVersionedRef4 = "temp_versioned_ref4";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef4,
                pgTable.getName().replace("\"", ""),
                sysPkColumn,
                "num");

        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef4,
                version3
        );

        //портим версию
        //удаляем 2 записи
        executeQuery("DELETE FROM versioned_ref_versions WHERE id = (SELECT max(id) from versioned_ref_versions)");
        executeQuery("DELETE FROM versioned_ref_versions WHERE id = (SELECT max(id) from versioned_ref_versions)");

        //добавляем 1 запись
        Long recId = 3L;
        Long num = 10L;
        versionedDataDao.insertVersions(List.of(recId), nextVersion2, pgTable);

        VersionedLocalDataCriteria criteria = new VersionedLocalDataCriteria(refbookCode, pgTable.getName().replace("\"", ""), "num", 100, 0, new ArrayList<>(), nextVersion2.toString());
        addPkFilter(criteria, num);
        Map<String, Object> rec = getDataByPkField(criteria);
        Assertions.assertNotNull(rec, "Запись не была добавлена.");

        //редактируем 2 записи
        executeQuery("UPDATE versioned_ref SET _sync_hash = '8691427f2bc711f891fd30d6942b6cd5' WHERE name = 'name6'");
        executeQuery("UPDATE versioned_ref SET _sync_hash = '45617f8f6aba83feb53fdb1503bee47e' WHERE name = 'name7'");


        //восстанавливаем версию
        versionedDataDao.repeatVersion(tempVersionedRef4, pgTable, nextVersion2);

        //проверка
        VersionedLocalDataCriteria localDataCriteria = new VersionedLocalDataCriteria(refbookCode, pgTable.getName().replace("\"", ""), "num", 100, 0, new ArrayList<>(), nextVersion2.toString());
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(9, data.getTotalElements());
        compare(data.getContent(), version3, localDataCriteria.getPk());

        rec = getDataByPkField(criteria);
        Assertions.assertNull(rec, "Запись не была удалена.");
    }

    private void compare(List<Map<String, Object>> data, List<Map<String, Object>> version, String pk) {
        data.forEach(res -> {
            Map<String, Object> versionRec = findRecordByPk(res, version, pk);
            Assertions.assertNotNull(versionRec);
            res.forEach((key, value) -> {
                if (versionRec.get(key) instanceof LocalDate) {
                    Assertions.assertEquals(versionRec.get(key), ((Date) value).toLocalDate());
                } else {
                    Assertions.assertEquals(versionRec.get(key), value);
                }
            });
        });
    }

    private Map<String, Object> findRecordByPk(Map<String, Object> record, List<Map<String, Object>> version, String pk) {
        for (Map<String, Object> versionRec : version) {
            if (versionRec.get(pk).equals(record.get(pk)))
                return versionRec;
        }
        return null;
    }

    private Map<String, Object> getDataByPkField(VersionedLocalDataCriteria localDataCriteria) {
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assert.isTrue(data.getTotalElements() <= 1, "Не может быть > 1 записи.");
        if (data.getTotalElements() == 0)
            return null;
        else
            return data.getContent().get(0);
    }

    private void addPkFilter(VersionedLocalDataCriteria localDataCriteria, Long pk) {
        FieldValueFilter fieldValueFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, List.of(pk));
        FieldFilter fieldFilter = new FieldFilter(localDataCriteria.getPk(), DataTypeEnum.INTEGER, List.of(fieldValueFilter));//todo брать тип из маппинга
        localDataCriteria.getFilters().add(fieldFilter);
    }

    private void executeQuery(String query) {
        namedParameterJdbcTemplate.getJdbcTemplate().execute(query);
    }

}
