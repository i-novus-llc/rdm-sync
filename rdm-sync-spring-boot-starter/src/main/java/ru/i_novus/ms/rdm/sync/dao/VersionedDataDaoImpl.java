package ru.i_novus.ms.rdm.sync.dao;

import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.dao.builder.SqlFilterBuilder;
import ru.i_novus.ms.rdm.sync.dao.criteria.BaseDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;
import ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.sync.util.StringUtils.addDoubleQuotes;
import static ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils.escapeName;

@Slf4j
public class VersionedDataDaoImpl implements VersionedDataDao {

    private static final String RECORD_PK_COL = "_sync_rec_id";
    private static final String RECORD_FROM_DT = "_sync_from_dt";
    private static final String RECORD_TO_DT = "_sync_to_dt";
    private static final String RECORD_HASH = "_sync_hash";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public VersionedDataDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public void addFirstVersionData(String tempTable,
                                    String refTable,
                                    String pkField,
                                    LocalDateTime fromDate,
                                    LocalDateTime toDate,
                                    List<String> fields) {
        String columnsExpression =  fields.stream().map(RdmSyncDataUtils::escapeName).collect(joining(","));
        String hashExpression = getHashExpression(fields, tempTable);
        String name = refTable.replace("\"", "");
        String intervalsTableName = name + "_intervals";
        Map<String, String> queryPlaceholders = Map.of("tempTbl", escapeName(tempTable),
                "columnsExpression", columnsExpression,
                "hashExpression", hashExpression,
                "hash", RECORD_HASH,
                "refTbl", escapeName(refTable),
                "pk", escapeName(pkField),
                "syncId", RECORD_PK_COL,
                "fromDt", RECORD_FROM_DT,
                "toDt", RECORD_TO_DT,
                "refTableIntervals", escapeName(intervalsTableName));

        //выбираем из refTbl syncId и вставляем в таблицу интервалов
        String dataQueryTemplate = "WITH table_data AS (" +
                "SELECT {syncId} FROM {refTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {pk} = {refTbl}.{pk})" +
                ") " +
                "INSERT INTO {refTableIntervals} (record_id, {fromDt}, {toDt}) " +
                "SELECT {syncId}, :from_dt, :to_dt " +
                "FROM table_data";

        String query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");
        Map<String, Object> map = new HashMap<>();
        map.put("from_dt", fromDate);
        map.put("to_dt", toDate);
        namedParameterJdbcTemplate.update(query, map);

        //вставляем записи в refTbl и в таблицу интервалов
        dataQueryTemplate = "WITH table_data AS (" +
                "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE NOT EXISTS(SELECT 1 FROM {refTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {tempTbl}.{pk} = {refTbl}.{pk})) RETURNING {syncId}" +
                ")" +
                "INSERT INTO {refTableIntervals} (record_id, {fromDt}, {toDt}) " +
                "SELECT {syncId}, :from_dt, :to_dt " +
                "FROM table_data";
        query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");
        namedParameterJdbcTemplate.update(query, map);
    }

    @Override
    public void addDiffVersionData(String tempTable,
                                   String refTable,
                                   String pkField,
                                   LocalDateTime fromDate,
                                   LocalDateTime toDate,
                                   List<String> fields) {
       addFirstVersionData(tempTable, refTable, pkField, fromDate, toDate, fields);
    }

    @Override
    public void repeatVersion(String tempTable,
                              String refTable,
                              String pkField,
                              LocalDateTime fromDate,
                              LocalDateTime toDate,
                              List<String> fields) {
        log.info("Begin repeat version for {} version {}", refTable, fromDate);
        String columnsExpression = fields.stream().map(RdmSyncDataUtils::escapeName).collect(joining(","));
        String hashExpression = getHashExpression(fields, tempTable);
        String name = refTable.replace("\"", "");
        String intervalsTableName = name + "_intervals";
        Map<String, String> queryPlaceholders = Map.ofEntries(
                Map.entry("tempTbl", escapeName(tempTable)),
                Map.entry("columnsExpression", columnsExpression),
                Map.entry("hashExpression", hashExpression),
                Map.entry("hash", escapeName(RECORD_HASH)),
                Map.entry("refTbl", escapeName(refTable)),
                Map.entry("pk", escapeName(pkField)),
                Map.entry("syncId", escapeName(RECORD_PK_COL)),
                Map.entry("refTableIntervals", escapeName(intervalsTableName)),
                Map.entry("fromDt", escapeName(RECORD_FROM_DT)),
                Map.entry("toDt", escapeName(RECORD_TO_DT)),
                Map.entry("uniqColumns", escapeName(pkField) + ", " + escapeName(RECORD_HASH)));

        //удаляем записи, которых нет в версии
        String deleteQueryTemplate = "SELECT {syncId} FROM {refTbl} JOIN {refTableIntervals} ON {refTbl}.{syncId} = {refTableIntervals}.record_id " +
                " WHERE NOT EXISTS(SELECT 1 FROM {tempTbl} WHERE {pk} = {refTbl}.{pk})" +
                " AND {refTableIntervals}.{fromDt} <= :from_date AND ({refTableIntervals}.{toDt} IS NULL OR {refTableIntervals}.{toDt} > :from_date)";
        String deleteQuery = StringSubstitutor.replace(deleteQueryTemplate, queryPlaceholders,"{", "}");
        List<Long> idsForDelete = namedParameterJdbcTemplate.queryForList(deleteQuery, Map.of("from_date", fromDate), Long.class);
        //


        //получаем записи, которые есть в temp, но нет в сохраненной версии
        String selectQueryTemplate = "SELECT {pk} FROM {tempTbl} " +
                "WHERE NOT EXISTS(SELECT 1 FROM {refTbl} JOIN {refTableIntervals} ON {refTbl}.{syncId} = {refTableIntervals}.record_id " +
                                 "WHERE {refTableIntervals}.{fromDt} <= :from_date AND ({refTableIntervals}.{toDt} IS NULL OR {refTableIntervals}.{toDt} > :from_date) " +
                                 "AND {pk} = {tempTbl}.{pk})";
        String selectQuery = StringSubstitutor.replace(selectQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> srcIds = namedParameterJdbcTemplate.queryForList(selectQuery, Map.of("from_date", fromDate), Long.class);


        List<Long> idsForInsert = new ArrayList<>();
        if (!srcIds.isEmpty()) {
            //добавляем запись в refTbl, если ее нет
            String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds)) ON CONFLICT ({uniqColumns}) DO NOTHING RETURNING {syncId}";
            String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class);

            //получаем _sync_rec_id
            insertQueryTemplate = "SELECT {syncId} FROM {refTbl} WHERE EXISTS (SELECT 1 FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds) AND {pk} = {refTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}))";
            query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            idsForInsert = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class);
        }
        //

        //редактируем записи, которые изменились

        //ищем записи, которые есть и в tempTbl и в сохраненной версии (по pk), но с разным hash
        String sqt = "SELECT {pk} FROM {tempTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {refTbl} JOIN {refTableIntervals} ON {refTbl}.{syncId} = {refTableIntervals}.record_id " +
                "WHERE {refTableIntervals}.{fromDt} <= :from_date AND ({refTableIntervals}.{toDt} IS NULL OR {refTableIntervals}.{toDt} > :from_date) " +
                "AND {refTbl}.{pk} = {tempTbl}.{pk} " +
                "AND {refTbl}.{hash} <> md5({hashExpression}))";
        String sq = StringSubstitutor.replace(sqt, queryPlaceholders, "{", "}");
        List<Long> srcIds2 = namedParameterJdbcTemplate.queryForList(sq, Map.of("from_date", fromDate), Long.class);

        if (!srcIds2.isEmpty()) {
            String updateQueryTemplate = "SELECT {syncId} FROM {refTbl} JOIN {refTableIntervals} ON {refTbl}.{syncId} = {refTableIntervals}.record_id " +
                    "WHERE {refTableIntervals}.{fromDt} <= :from_date AND ({refTableIntervals}.{toDt} IS NULL OR {refTableIntervals}.{toDt} > :from_date) " +
                    "AND {pk} in (:srcIds2)";
            String updateQuery = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders, "{", "}");
            List<Long> idsForDelete2 = namedParameterJdbcTemplate.queryForList(updateQuery, Map.of("from_date", fromDate, "srcIds2", srcIds2), Long.class);
            idsForDelete.addAll(idsForDelete2);

            String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds2)) ON CONFLICT ({uniqColumns}) DO NOTHING RETURNING {syncId}";
            String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            namedParameterJdbcTemplate.queryForList(query, Map.of("from_date", fromDate, "srcIds2", srcIds2), Long.class);


            //получаем _sync_rec_id
            insertQueryTemplate = "SELECT {syncId} FROM {refTbl} WHERE EXISTS (SELECT 1 FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds2) AND {pk} = {refTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}))";
            query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            List<Long> idsForInsert2 = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class);
            idsForInsert.addAll(idsForInsert2);
        }

        //

        insertIntervals(idsForInsert, fromDate, toDate, refTable);
        deleteIntervals(idsForDelete, fromDate, toDate, refTable);

        log.info("End repeat version for {} version {}", refTable, fromDate);
    }

    private void deleteIntervals(List<Long> ids,
                                     LocalDateTime fromDate,
                                     LocalDateTime toDate,
                                     String refTable) {

        Map<String, Object> params = new HashMap<>();
        params.put("fromDt", fromDate);
        params.put("toDt", toDate);

        String name = refTable.replace("\"", "");
        String intervalsTableName = name + "_intervals";
        Map<String, String> queryPlaceholders = Map.of(
                "fromDt", RECORD_FROM_DT,
                "toDt", RECORD_TO_DT,
                "refTableIntervals", escapeName(intervalsTableName));

        ids.forEach(id -> {
            params.put("id", id);
            List<Map<String, Object>> intervalData = getDataAsMap(StringSubstitutor.replace("SELECT * FROM {refTableIntervals} " +
                    "WHERE record_id = :id AND {fromDt} <= :fromDt AND ({toDt} IS NULL OR {toDt} > :fromDt)", queryPlaceholders,"{", "}"), params);
            Assert.isTrue(intervalData.size() == 1, "There must be one interval.");
            Map<String, Object> interval = intervalData.get(0);

            LocalDateTime intervalStart = (LocalDateTime) interval.get(RECORD_FROM_DT);
            LocalDateTime intervalEnd = (LocalDateTime) interval.get(RECORD_TO_DT);
            Long intervalId = (Long) interval.get("id");
            Long recordId = (Long) interval.get("record_id");

            if (intervalStart.equals(fromDate) && intervalEnd.equals(toDate)) {
                namedParameterJdbcTemplate.update(StringSubstitutor.replace("DELETE FROM {refTableIntervals} WHERE id = :intervalId", queryPlaceholders,"{", "}"),
                        Map.of("intervalId", intervalId));
            } else if (intervalStart.equals(fromDate)) {

                namedParameterJdbcTemplate.update(StringSubstitutor.replace("UPDATE {refTableIntervals} SET {fromDt} = :fromDt WHERE id = :intervalId", queryPlaceholders,"{", "}"),
                        Map.of("fromDt", toDate, "intervalId", intervalId));

            } else if (intervalEnd.equals(toDate)) {

                namedParameterJdbcTemplate.update(StringSubstitutor.replace("UPDATE {refTableIntervals} SET {toDt} = :toDt WHERE id = :intervalId", queryPlaceholders,"{", "}"),
                        Map.of("toDt", fromDate, "intervalId", intervalId));

            } else {

                namedParameterJdbcTemplate.update(StringSubstitutor.replace("UPDATE {refTableIntervals} SET {toDt} = :toDt WHERE id = :intervalId", queryPlaceholders,"{", "}"),
                        Map.of("toDt", fromDate, "intervalId", intervalId));

                insertIntervals(List.of(recordId), toDate, intervalEnd, refTable);

            }
        });
    }

    private String getHashExpression(List<String> fields, String table) {
        return fields.stream().map(field -> "coalesce(" + escapeName(table) + "." + escapeName(field) + "::text, '')").collect(joining("||"));
    }

    public void insertIntervals(List<Long> ids,
                                 LocalDateTime fromDate,
                                 LocalDateTime toDate,
                                 String refTable) {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDt", fromDate);
        params.put("toDt", toDate);
        ids.forEach(id -> {
            params.put("id", id);
            List<Long> dateIds = namedParameterJdbcTemplate.queryForList("INSERT INTO " + escapeName(refTable + "_intervals") + "(record_id, " + RECORD_FROM_DT + ", " + RECORD_TO_DT + ") " +
                    "VALUES (:id, :fromDt, :toDt) RETURNING id", params, Long.class);
            log.info(dateIds.toString());
        });
    }

    @Override
    public Page<Map<String, Object>> getData(LocalDataCriteria localDataCriteria) {
        Map<String, Object> args = new HashMap<>();
        String name = localDataCriteria.getSchemaTable().replace("\"", "");
        name = name + "_intervals";
        String sql = String.format("%n  FROM %s %n JOIN %s ON %s._sync_rec_id = %s.record_id WHERE 1=1 %n",
                escapeName(localDataCriteria.getSchemaTable()), escapeName(name), escapeName(localDataCriteria.getSchemaTable()), escapeName(name));

        if(localDataCriteria.getDateTime() != null) {
            sql = sql + " AND _sync_from_dt <= :dt AND (_sync_to_dt IS NULL OR _sync_to_dt > :dt)";
            args.put("dt", localDataCriteria.getDateTime());
        }


        Page<Map<String, Object>> data = getData0(sql, args, localDataCriteria, null);
        data.getContent().forEach(row -> {
            row.remove(RECORD_PK_COL);
            row.remove(RECORD_FROM_DT);
            row.remove(RECORD_TO_DT);
            row.remove(RECORD_HASH);
            row.remove("record_id");
            row.remove("id");
            row.remove("rdm_sync_internal_local_row_state");
        });

        return data;
    }

    @Override
    public void addPkFilter(LocalDataCriteria localDataCriteria, Long pk) {
        FieldValueFilter fieldValueFilter = new FieldValueFilter(FilterTypeEnum.EQUAL, List.of(pk));
        FieldFilter fieldFilter = new FieldFilter(localDataCriteria.getPk(), DataTypeEnum.INTEGER, List.of(fieldValueFilter));//todo брать тип из маппинга
        localDataCriteria.getFilters().add(fieldFilter);
    }

    @Override
    public Map<String, Object> getDataByPkField(LocalDataCriteria localDataCriteria) {
        Page<Map<String, Object>> data = getData(localDataCriteria);
        Assert.isTrue(data.getTotalElements() <= 1, "Не может быть > 1 записи.");
        if (data.getTotalElements() == 0)
            return null;
        else
            return data.getContent().get(0);
    }

    private Page<Map<String, Object>> getData0(String sql, Map<String, Object> args, BaseDataCriteria dataCriteria, String selectSubQuery) {

        SqlFilterBuilder filterBuilder = getFiltersClause(dataCriteria.getFilters());
        if (filterBuilder != null) {
            sql += "\n AND " + filterBuilder.build();
            args.putAll(filterBuilder.getParams());
        }

        Integer count = namedParameterJdbcTemplate.queryForObject("SELECT count(*)" + sql, args, Integer.class);
        if (count == null || count == 0)
            return Page.empty();

        int limit = dataCriteria.getLimit();
        if (limit != 1) {
            sql += String.format("%n ORDER BY %s ", addDoubleQuotes(dataCriteria.getPk()));
        }

        sql += String.format("%n LIMIT %d OFFSET %d", limit, dataCriteria.getOffset());

        sql = "SELECT * " + (selectSubQuery != null ? ", " + selectSubQuery + " " : "")  + sql;

        if (log.isDebugEnabled()) {
            log.debug("getData0 sql:\n{}\n binding args:\n{}\n.", sql, args);
        }

        List<Map<String, Object>> result = namedParameterJdbcTemplate.query(sql,
                args, (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        Object val = rs.getObject(i);
                        String key = rs.getMetaData().getColumnName(i);
                        if (val instanceof Timestamp) {
                            val = ((Timestamp) val).toLocalDateTime();
                        } else if( val instanceof Array) {
                            val = Arrays.asList((Object[]) ((Array)val).getArray());
                        }
                        map.put(key, val);
                    }

                    return map;
                });

        final RestCriteria restCriteria = new DataCriteria();
        restCriteria.setPageNumber(dataCriteria.getOffset() / limit);
        restCriteria.setPageSize(limit);
        restCriteria.setOrders(Sort.by(Sort.Order.asc(dataCriteria.getPk())).get().collect(toList()));

        return new PageImpl<>(result, restCriteria, count);
    }

    /**
     * Получение условия для фильтра по полям.
     */
    private SqlFilterBuilder getFiltersClause(List<FieldFilter> filters) {

        if (CollectionUtils.isEmpty(filters))
            return null;

        SqlFilterBuilder builder = new SqlFilterBuilder();
        filters.forEach(builder::parse);

        return builder;
    }

    @Override
    public List<Map<String, Object>> getDataAsMap(String sql, Map<String, Object> args) {
        return namedParameterJdbcTemplate.query(sql,
                args, (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        Object val = rs.getObject(i);
                        String key = rs.getMetaData().getColumnName(i);
                        if (val instanceof Timestamp) {
                            val = ((Timestamp) val).toLocalDateTime();
                        } else if( val instanceof Array) {
                            val = Arrays.asList((Object[]) ((Array)val).getArray());
                        }
                        map.put(key, val);
                    }

                    return map;
                });
    }

    @Override
    public void executeQuery(String query) {
        namedParameterJdbcTemplate.getJdbcTemplate().execute(query);
    }

    @Override
    public void updateQuery(String query, Map<String, Object> map) {
        namedParameterJdbcTemplate.getJdbcTemplate().update(query, map);
    }

    @Override
    @Transactional
    public void mergeIntervals(String refTable) {

        log.info("Start merge intervals for table {}.", refTable);
        String query = """
                DROP TABLE IF EXISTS %s;
                CREATE TABLE %s (
                id BIGSERIAL PRIMARY KEY,
                record_id BIGINT NOT NULL,
                _sync_from_dt TIMESTAMP NOT NULL,
                _sync_to_dt TIMESTAMP,
                CONSTRAINT record_id_fk FOREIGN KEY (record_id) REFERENCES %s (_sync_rec_id) ON DELETE NO ACTION ON UPDATE NO ACTION
              );
              """;

        String name = refTable.replace("\"", "");
        String tempTableName = name + "_intervals_temp";
        String intervalsTableName = name + "_intervals";
        executeQuery(String.format(query, escapeName(tempTableName), escapeName(tempTableName), refTable));


        String sql = "INSERT INTO %s(_sync_from_dt, _sync_to_dt, record_id) SELECT \n" +
                "  MIN(" + RECORD_FROM_DT + ") AS merged_start,\n" +
                "  MAX(" + RECORD_TO_DT + ") AS merged_end,\n" +
                "  record_id\n" +
                "FROM (\n" +
                "  SELECT \n" +
                "    *,\n" +
                "    SUM(step) OVER (PARTITION BY record_id ORDER BY " + RECORD_FROM_DT + ") AS grp\n" +
                "  FROM (\n" +
                "    SELECT \n" +
                "      *,\n" +
                "      CASE WHEN " + RECORD_FROM_DT + " > MAX(" + RECORD_TO_DT + ") OVER (PARTITION BY record_id\n" +
                "             ORDER BY " + RECORD_FROM_DT + " \n" +
                "             ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING\n" +
                "           ) \n" +
                "           THEN 1 WHEN " + RECORD_TO_DT + " IS NULL THEN 1 ELSE 0 END AS step\n" +
                "    FROM %s\n" +
                "  ) t\n" +
                ") t2\n" +
                "GROUP BY grp, record_id;";

        executeQuery(String.format(sql, escapeName(tempTableName), escapeName(intervalsTableName)));

        String sqlDrop = "DROP TABLE %s";
        executeQuery(String.format(sqlDrop, escapeName(intervalsTableName)));
        String sqlRename = "ALTER TABLE %s RENAME TO %s";
        String intervalsTableNameWithoutSchema = intervalsTableName.substring(intervalsTableName.indexOf(".") + 1);
        executeQuery(String.format(sqlRename, escapeName(tempTableName), escapeName(intervalsTableNameWithoutSchema)));

        log.info("End merge intervals for table {}.", refTable);
    }

    @Override
    public void closeIntervals(String refTable, LocalDateTime closedVersionPublishingDate, LocalDateTime newVersionPublishingDate) {
        log.info("Start close intervals for table {}.", refTable);
        String name = refTable.replace("\"", "");
        String intervalsTableName = name + "_intervals";

        String sql = "UPDATE %s SET _sync_to_dt = :toDt WHERE _sync_to_dt IS NULL AND _sync_from_dt = :fromDt";

        namedParameterJdbcTemplate.update(String.format(sql, escapeName(intervalsTableName)),
                Map.of("fromDt", closedVersionPublishingDate,
                        "toDt", newVersionPublishingDate)
        );
        log.info("End close intervals for table {}.", refTable);
    }
}
