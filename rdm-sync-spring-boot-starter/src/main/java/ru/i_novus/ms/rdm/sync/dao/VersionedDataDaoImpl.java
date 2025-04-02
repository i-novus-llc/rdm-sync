package ru.i_novus.ms.rdm.sync.dao;

import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.dao.builder.SqlFilterBuilder;
import ru.i_novus.ms.rdm.sync.dao.criteria.BaseDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
        Map<String, String> queryPlaceholders = Map.of("tempTbl", escapeName(tempTable),
                "columnsExpression", columnsExpression,
                "hashExpression", hashExpression,
                "hash", RECORD_HASH,
                "refTbl", escapeName(refTable),
                "pk", escapeName(pkField),
                "syncId", RECORD_PK_COL);

        String selectQueryTemplate = "SELECT {syncId} FROM {refTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {pk} = {refTbl}.{pk});";
        String query = StringSubstitutor.replace(selectQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> idsExisted = namedParameterJdbcTemplate.queryForList(query, new HashMap<>(), Long.class);

        String updateQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE NOT EXISTS(SELECT 1 FROM {refTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {tempTbl}.{pk} = {refTbl}.{pk})) RETURNING {syncId};";
        query = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> ids = namedParameterJdbcTemplate.queryForList(query, new HashMap<>(), Long.class);

        ids.addAll(idsExisted);

        insertIntervals(ids, fromDate, toDate, refTable);
        log.info(ids.toString());
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
        String columnsExpression = fields.stream().map(RdmSyncDataUtils::escapeName).collect(joining(","));
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
                "refTableIntervals", escapeName(intervalsTableName));

        //удаляем записи которых нет в версии
        String deleteQueryTemplate = "SELECT {syncId} FROM {refTbl} JOIN {refTableIntervals} ON {refTbl}.{syncId} = {refTableIntervals}.record_id " +
                " WHERE NOT EXISTS(SELECT 1 FROM {tempTbl} WHERE {pk} = {refTbl}.{pk})" +
                " AND {refTableIntervals}.from_dt <= :fromDate AND ({refTableIntervals}.to_dt IS NULL OR {refTableIntervals}.to_dt > :from_date)";
        String deleteQuery = StringSubstitutor.replace(deleteQueryTemplate, queryPlaceholders,"{", "}");
        List<Long> idsForDelete = namedParameterJdbcTemplate.queryForList(deleteQuery, new HashMap<>(), Long.class);
        //


        //получаем записи которые есть в temp, но нет в сохраненной версии
        String selectQueryTemplate = "SELECT {pk} FROM {tempTbl} " +
                "WHERE NOT exists(SELECT 1 FROM {refTbl} JOIN {refTableIntervals} ON {refTbl}.{syncId} = {refTableIntervals}.record_id " +
                                 "WHERE {pk} = {tempTbl}.{pk} " +
                                 "AND {refTableIntervals}.from_dt <= :fromDate AND ({refTableIntervals}.to_dt IS NULL OR {refTableIntervals}.to_dt > :from_date))";
        String selectQuery = StringSubstitutor.replace(selectQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> srcIds = namedParameterJdbcTemplate.queryForList(selectQuery, new HashMap<>(), Long.class);


        //добавляем запись в refTbl, если ее нет в refTbl
        String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE {pk} in :srcIds AND NOT EXISTS(SELECT 1 FROM {refTbl} WHERE {pk} = {tempTbl}.{pk} AND {hash} = md5({hashExpression}))) RETURNING {syncId};";
        String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> ids = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class); //не нужно возвращать айдишники

        //получаем _sync_rec_id
        insertQueryTemplate = "SELECT {syncId} FROM {refTbl} WHERE EXISTS (SELECT 1 FROM {tempTbl} " +
                "WHERE {pk} in :srcIds AND {pk} = {refTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression})) RETURNING {syncId};";
        query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> idsForInsert = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class);

        //

        //редактируем записи которые изменились
        String updateQueryTemplate = "SELECT {syncId} FROM {refTable} JOIN {refTblIntervals} ON {refTable}.{syncId} = {refTblIntervals}.record_id " +
                "WHERE exists(SELECT 1 FROM {tempTbl} WHERE {pk} = {refTbl}.{pk} AND md5({hashExpression}) <> {refTbl}.{hash})";
        String updateQuery = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders,"{", "}");
        List<Long> idsForDelete2 = namedParameterJdbcTemplate.queryForList(updateQuery, new HashMap<>(), Long.class);
        idsForDelete.addAll(idsForDelete2);

        insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {refTbl} JOIN {refTblIntervals} ON {refTable}.{syncId} = {refTblIntervals}.record_id WHERE {pk} = {tempTbl}.{pk} AND {hash} <> md5({hashExpression}))) RETURNING {syncId};";
        query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> idsForInsert2 = namedParameterJdbcTemplate.queryForList(query, new HashMap<>(), Long.class);
        idsForInsert.addAll(idsForInsert2);

        //

        insertIntervals(idsForInsert, fromDate, toDate, refTable);
        deleteIntervals(idsForDelete, fromDate, toDate, refTable);
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
            List<Long> intervalIds = namedParameterJdbcTemplate.queryForList("SELECT id FROM {refTableIntervals} " +
                    "WHERE record_id = :id AND {fromDt} <= :fromDt AND {toDt} > :fromDt", params, Long.class);
            Assert.isTrue(intervalIds.size() == 1, "There must be one interval.");
            Long intervalId = intervalIds.get(0);
            namedParameterJdbcTemplate.update("UPDATE {refTableIntervals} SET {toDt} = :toDt WHERE id = :intervalId",
                    Map.of("toDt", fromDate, "intervalId", intervalId));
//           insertIntervals(List.of(id), toDate);//todo
        });
    }

    private String getHashExpression(List<String> fields, String table) {
        return fields.stream().map(field -> "coalesce(" + escapeName(table) + "." + escapeName(field) + "::text, '')").collect(joining("||"));
    }

    private void insertIntervals(List<Long> ids,
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
//            row.remove(RECORD_FROM_DT);
//            row.remove(RECORD_TO_DT);
//            row.remove(RECORD_HASH);
        });

        return data;
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
    public List<Map<String, Object>> getDataFromTmp(String sql, Map<String, Object> args) {
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
    public void mergeIntervals(String refTable) {
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
        executeQuery(String.format(sqlRename, escapeName(tempTableName), escapeName(intervalsTableName)));
    }

    @Override
    public void closeIntervals(String refTable, LocalDateTime closedVersionPublishingDate, LocalDateTime newVersionPublishingDate) {
        String name = refTable.replace("\"", "");
        String intervalsTableName = name + "_intervals";

        String sql = "UPDATE %s SET _sync_to_dt = :toDt WHERE _sync_to_dt IS NULL AND _sync_from_dt = :fromDt";

        namedParameterJdbcTemplate.update(String.format(sql, escapeName(intervalsTableName)),
                Map.of("fromDt", closedVersionPublishingDate,
                        "toDt", newVersionPublishingDate)
        );
    }
}
