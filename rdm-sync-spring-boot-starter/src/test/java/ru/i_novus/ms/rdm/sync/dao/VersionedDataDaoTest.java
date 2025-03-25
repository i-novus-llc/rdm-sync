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
import java.util.List;
import java.util.Map;

@JdbcTest(properties = {
        "spring.liquibase.enabled=false"
})
@Sql(scripts = {"/versionedDataDaoTest.sql"})
public class VersionedDataDaoTest extends BaseDaoTest {

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
                        .primaryField("number")
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
        versionedDataDao.addFirstVersionData(tempVersionedRef, refTable, "num", firstVersionDate, null, fields);

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(refTable, "num", 100, 0, new ArrayList<>());
        Page<Map<String, Object>> data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(2, data.getTotalElements());

        // фильтруем по дате вне версии
        localDataCriteria.setDateTime(firstVersionDate.minus(1, ChronoUnit.DAYS));
        data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(0, data.getTotalElements());

        //следующая версия
        String diffTable = "tempDiffTbl";
        rdmSyncDao.createDiffTempDataTbl(diffTable, refTable);
        List<Map<String, Object>> inserted = List.of(
                Map.of("name", "name3", "num", 3, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name4", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt),
                Map.of("name", "name5", "num", 5, "flag", false, "some_dt", secondVersionCreatedDt)
        );
        List<Map<String, Object>> updated = List.of(
                Map.of("name", "name2 updated", "num", 4, "flag", false, "some_dt", secondVersionCreatedDt)
        );

        List<Map<String, Object>> deleted = List.of(
                Map.of("name", "name1", "num", 1, "flag", true, "some_dt", firsVersionCreatedDt)
        );

        rdmSyncDao.insertDiffAsTempData(diffTable, inserted, updated, deleted);
        LocalDateTime nextVersionDt = LocalDateTime.of(2025, 3, 24, 0, 0);
        versionedDataDao.addDiffVersionData(diffTable, refTable, "num", nextVersionDt, null, fields);

        localDataCriteria.setDateTime(nextVersionDt.plusHours(1));
        data = versionedDataDao.getData(localDataCriteria);
        Assertions.assertEquals(4, data.getTotalElements());

    }

    // todo
//    при применении diff важен порядок. лучше сначало обрабатывать удаленные, потом все остальные
//    при применении diff учитывать используемые поля, т.е может быть так что изменение в справочнике произошло в атрибуте который не используется в маппинге,
//    тогда в таком случае в diff придет информация о том что запись изменилась но у нас по факту она не изменилась т.к. нет такой колонки
}
