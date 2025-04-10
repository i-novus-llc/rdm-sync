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
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.init.dao.pg.impl.VersionedLocalRefBookCreatorDao;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final LocalDateTime firstVersionDate = LocalDateTime.of(2025, 3, 23, 0, 0);
    private static final LocalDateTime nextVersionDate = LocalDateTime.of(2025, 3, 24, 0, 0);
    private static final LocalDateTime nextVersionDate2 = LocalDateTime.of(2025, 3, 25, 0, 0);
    private static final LocalDateTime nextVersionDate3 = LocalDateTime.of(2025, 3, 27, 0, 0);

    private static final LocalDate secondVersionCreatedDt = LocalDate.now();
    private static final LocalDate firstVersionCreatedDt = secondVersionCreatedDt.minusDays(3);

    private static final String tempVersionedRef3 = "temp_versioned_ref3";
    private static List<Map<String, Object>> version3;

    @Configuration
    static class Config {

        @Bean
        RdmSyncDao rdmSyncDao() {
            return new RdmSyncDaoImpl();
        }

        @Bean
        VersionedDataDao versionedDataDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
            return new VersionedDataDaoImpl(namedParameterJdbcTemplate);
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

    /**
     * Тестирует CRUD операции версионных данных
     */
    @Test
    @Order(1)
    void testCRUDVersionedData() {
        List<FieldMapping> fieldMappings = List.of(
                new FieldMapping("name", "varchar", "name"),
                new FieldMapping("num", "integer", "num"),
                new FieldMapping("flag", "boolean", "num"),
                new FieldMapping("some_dt", "date", "some_dt")
        );
        List<String> fields = fieldMappings.stream().map(FieldMapping::getSysField).toList();
        String refTable = "versioned_ref";
        versionedLocalRefBookCreatorDao.createTable(
                refTable,
                "test",
                VersionMapping.builder()
                        .primaryField("num")
                        .build(),
                fieldMappings,
                "some table",
                Map.of()
        );

        loadFirstVersion(refTable, fields);

        //загружаем следующую версию
        loadSecondVersion(refTable, fields);

        //следующая версия
        loadThirdVersion(refTable, fields);

        //repeatVersion
        repeatVersion(refTable, fields);
    }

    private void loadFirstVersion(String refTable,
                                  List<String> fields) {
        String tempVersionedRef = "temp_versioned_ref";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef,
                refTable,
                "_sync_rec_id",
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
        versionedDataDao.addFirstVersionData(tempVersionedRef, refTable, "num", firstVersionDate, nextVersionDate, fields);
        versionedDataDao.mergeIntervals(refTable);

        //получаем данные справочника первой версии
        LocalDataCriteria localDataCriteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        localDataCriteria.setDateTime(firstVersionDate);
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(3, data.getTotalElements());
        compare(data.getContent(), version1, localDataCriteria.getPk());

        // фильтруем по дате вне версии
        localDataCriteria.setDateTime(firstVersionDate.minusDays(1));
        data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(0, data.getTotalElements());
    }

    private void loadSecondVersion(String refTable,
                                   List<String> fields) {
        String tempVersionedRef2 = "temp_versioned_ref2";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef2,
                refTable,
                "_sync_rec_id",
                "num");

        List<Map<String, Object>> version2 = List.of(
                Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firstVersionCreatedDt),
                Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef2,
                version2
        );

        versionedDataDao.addDiffVersionData(tempVersionedRef2, refTable, "num", nextVersionDate, nextVersionDate2, fields);
        versionedDataDao.mergeIntervals(refTable);

        //проверка
        LocalDataCriteria localDataCriteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        localDataCriteria.setDateTime(nextVersionDate);
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(5, data.getTotalElements());
        compare(data.getContent(), version2, localDataCriteria.getPk());


        List<Map<String, Object>> mergeResult = versionedDataDao.getDataAsMap("select * from versioned_ref_intervals", new HashMap<>());
        Assertions.assertEquals(7, mergeResult.size());
    }

    private void loadThirdVersion(String refTable,
                                  List<String> fields) {

        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef3,
                refTable,
                "_sync_rec_id",
                "num");

        version3 = List.of(
                Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firstVersionCreatedDt),
                Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef3,
                version3
        );

        versionedDataDao.addDiffVersionData(tempVersionedRef3, refTable, "num", nextVersionDate2, null, fields);
        versionedDataDao.mergeIntervals(refTable);

        List<Map<String, Object>> mergeResult = versionedDataDao.getDataAsMap("select * from versioned_ref_intervals", new HashMap<>());
        Assertions.assertEquals(12, mergeResult.size());

        versionedDataDao.closeIntervals(refTable, nextVersionDate2, nextVersionDate3);

        versionedDataDao.mergeIntervals(refTable);
        mergeResult = versionedDataDao.getDataAsMap("select * from versioned_ref_intervals", new HashMap<>());
        Assertions.assertEquals(7, mergeResult.size());

        //проверка
        LocalDataCriteria localDataCriteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        localDataCriteria.setDateTime(nextVersionDate2);
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(5, data.getTotalElements());
        compare(data.getContent(), version3, localDataCriteria.getPk());
    }

    private void repeatVersion(String refTable,
                               List<String> fields) {
        //портим версию
        versionedDataDao.executeQuery("DELETE FROM versioned_ref_intervals WHERE id = (SELECT max(id) from versioned_ref_intervals)");

        Long recId = 3L;
        Long num = 10L;
        versionedDataDao.insertIntervals(List.of(recId), nextVersionDate2, nextVersionDate3, refTable);

        LocalDataCriteria criteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        criteria.setDateTime(nextVersionDate2);
        versionedDataDao.addPkFilter(criteria, num);
        Map<String, Object> rec = versionedDataDao.getDataByPkField(criteria);
        Assertions.assertNotNull(rec, "Запись не была добавлена.");

        //восстанавливаем версию
        versionedDataDao.repeatVersion(tempVersionedRef3, refTable, "num", nextVersionDate2, nextVersionDate3, fields);

        //проверка
        LocalDataCriteria localDataCriteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        localDataCriteria.setDateTime(nextVersionDate2);
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(5, data.getTotalElements());
        compare(data.getContent(), version3, localDataCriteria.getPk());

        rec = versionedDataDao.getDataByPkField(criteria);
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

}
