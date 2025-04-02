package ru.i_novus.ms.rdm.sync.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JdbcTest(properties = {
        "spring.liquibase.enabled=false"
})
@Sql(scripts = {"/versionedDataDaoTest.sql"})
public class VersionedDataDaoTest extends BaseDaoTest {

    private static final String RECORD_FROM_DT = "_sync_from_dt";
    private static final String RECORD_TO_DT = "_sync_to_dt";

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
        String tempVersionedRef = "temp_versioned_ref";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef,
                refTable,
                "_sync_rec_id",
                "num");
        LocalDateTime secondVersionCreatedDt = LocalDateTime.now();
        LocalDateTime firsVersionCreatedDt = secondVersionCreatedDt;
        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef,
                List.of(
                        Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firsVersionCreatedDt),
                        Map.of("name", "name2", "num", 2, "flag", false, "some_dt", firsVersionCreatedDt)
                )
        );
        LocalDateTime firstVersionDate = LocalDateTime.of(2025, 3, 23, 0, 0);
        LocalDateTime nextVersionDt = LocalDateTime.of(2025, 3, 24, 0, 0);
        versionedDataDao.addFirstVersionData(tempVersionedRef, refTable, "num", firstVersionDate, nextVersionDt, fields);

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        localDataCriteria.setDateTime(firstVersionDate);
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(2, data.getTotalElements());

        // фильтруем по дате вне версии
        localDataCriteria.setDateTime(firstVersionDate.minus(1, ChronoUnit.DAYS));
        data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(0, data.getTotalElements());

        //следующая версия
        String tempVersionedRef2 = "temp_versioned_ref2";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef2,
                refTable,
                "_sync_rec_id",
                "num");
        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef2,
                List.of(
                        Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firsVersionCreatedDt),
                        Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt),
                        Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                        Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                        Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
                )
        );


        LocalDateTime nextVersionDt2 = LocalDateTime.of(2025, 3, 25, 0, 0);
        versionedDataDao.addDiffVersionData(tempVersionedRef2, refTable, "num", nextVersionDt, nextVersionDt2, fields);

        localDataCriteria.setDateTime(null);
        data = versionedDataDao.getData(localDataCriteria);

        List<Map<String, Object>> result = versionedDataDao.getDataFromTmp("select * from temp_versioned_ref2", new HashMap<>());

        localDataCriteria.setDateTime(nextVersionDt.plusHours(1));
        data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(5, data.getTotalElements());


        versionedDataDao.mergeIntervals(refTable);
        List<Map<String, Object>> mergeResult = versionedDataDao.getDataFromTmp("select * from versioned_ref_intervals", new HashMap<>());

        Assertions.assertEquals(6, mergeResult.size());
        Map<String, Object> mapForFirstRec = mergeResult.stream().filter(m -> m.get("record_id").equals(1L)).toList().get(0);
        Assertions.assertEquals(firstVersionDate, mapForFirstRec.get(RECORD_FROM_DT));
        Assertions.assertEquals(nextVersionDt2, mapForFirstRec.get(RECORD_TO_DT));

        Map<String, Object> mapForSecondRec = mergeResult.stream().filter(m -> m.get("record_id").equals(2L)).toList().get(0);
        Assertions.assertEquals(firstVersionDate, mapForSecondRec.get(RECORD_FROM_DT));
        Assertions.assertEquals(nextVersionDt, mapForSecondRec.get(RECORD_TO_DT));


        //следующая версия
        String tempVersionedRef3 = "temp_versioned_ref3";
        rdmSyncDao.createVersionTempDataTbl(
                tempVersionedRef3,
                refTable,
                "_sync_rec_id",
                "num");
        rdmSyncDao.insertVersionAsTempData(
                tempVersionedRef3,
                List.of(
                        Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firsVersionCreatedDt),
                        Map.of("name", "name2 updated", "num", 2, "flag", false, "some_dt", secondVersionCreatedDt),
                        Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                        Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                        Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
                )
        );


        versionedDataDao.addDiffVersionData(tempVersionedRef3, refTable, "num", nextVersionDt2, null, fields);

        versionedDataDao.mergeIntervals(refTable);
        mergeResult = versionedDataDao.getDataFromTmp("select * from versioned_ref_intervals", new HashMap<>());
        List<Map<String, Object>> all = versionedDataDao.getDataFromTmp("select * from versioned_ref", new HashMap<>());
        Assertions.assertEquals(11, mergeResult.size());

        LocalDateTime nextVersionDt3 = LocalDateTime.of(2025, 3, 27, 0, 0);
        versionedDataDao.closeIntervals(refTable, nextVersionDt2, nextVersionDt3);

        versionedDataDao.mergeIntervals(refTable);
        mergeResult = versionedDataDao.getDataFromTmp("select * from versioned_ref_intervals", new HashMap<>());
        Assertions.assertEquals(6, mergeResult.size());
    }


}
