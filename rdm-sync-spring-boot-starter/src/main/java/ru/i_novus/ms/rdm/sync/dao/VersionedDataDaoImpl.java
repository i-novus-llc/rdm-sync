package ru.i_novus.ms.rdm.sync.dao;

import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.dao.builder.SqlFilterBuilder;
import ru.i_novus.ms.rdm.sync.dao.criteria.BaseDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.api.model.PgTable;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;

import java.sql.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.sync.util.StringUtils.addDoubleQuotes;
import static ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils.escapeName;

@Slf4j
public class VersionedDataDaoImpl implements VersionedDataDao {

    private static final String RECORD_PK_COL = "_sync_rec_id";
    private static final String VERSION_ID = "version_id";
    private static final String RECORD_HASH = "_sync_hash";

    private static final int BATCH_SIZE = 1000;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final RdmSyncDao rdmSyncDao;

    public VersionedDataDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate, RdmSyncDao rdmSyncDao) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.rdmSyncDao = rdmSyncDao;
    }

    @Override
    public void addFirstVersionData(String tempTable,
                                    PgTable pgTable,
                                    Integer versionId) {
        Map<String, String> queryPlaceholders = getQueryPlaceholders(tempTable, pgTable);

        Map<String, Object> map = new HashMap<>();
        map.put("version_id", versionId);

        //вставляем записи в refTbl
        String dataQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) " +
                "SELECT {tempColumnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "LEFT JOIN {refTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}) " +
                "WHERE {refTbl}.{pk} IS NULL";
        String query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");

        long startTime = System.currentTimeMillis();
        int inserted = namedParameterJdbcTemplate.update(query, map);
        long duration = System.currentTimeMillis() - startTime;
        log.info("addFirstVersionData: Inserted {} records into refTbl in {} ms", inserted, duration);

        //выбираем из refTbl syncId и вставляем в таблицу версий
        dataQueryTemplate = "WITH table_data AS (" +
                "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                "INNER JOIN {tempTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression})" +
                ") " +
                "INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                "SELECT {syncId}, :version_id " +
                "FROM table_data";
        query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");

        startTime = System.currentTimeMillis();
        int linked = namedParameterJdbcTemplate.update(query, map);
        duration = System.currentTimeMillis() - startTime;
        log.info("addFirstVersionData: Linked {} records with version {} in {} ms", linked, versionId, duration);
    }

    @Override
    public void addDiffVersionData(String tempTable,
                                   PgTable pgTable,
                                   String code,
                                   Integer versionId,
                                   String syncedVersion) {

        Map<String, String> queryPlaceholders = getQueryPlaceholders(tempTable, pgTable);

        LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(code, syncedVersion);

        //добавляем неизмененные записи из предыдущей версии
        String str = "WITH table_data AS (" +
                "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                "INNER JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "LEFT JOIN {tempTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} " +
                "WHERE {refTableVersions}.{versionId} = :prev_version_id " +
                "AND {tempTbl}.{pk} IS NULL" +
                ") " +
                "INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                "SELECT {syncId}, :version_id " +
                "FROM table_data";

        String query = StringSubstitutor.replace(str, queryPlaceholders, "{", "}");
        Map<String, Object> map = new HashMap<>();
        map.put("version_id", versionId);
        map.put("prev_version_id", loadedVersion.getId());

        long startTime = System.currentTimeMillis();
        int rowsInserted = namedParameterJdbcTemplate.update(query, map);
        long duration = System.currentTimeMillis() - startTime;
        log.info("addDiffVersionData: Inserted {} unchanged records in {} ms", rowsInserted, duration);

        //обработка измененных записей(добавление и редактирование)
        String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE (diff_type = 'I' OR diff_type = 'U')) ON CONFLICT ({uniqColumns}) DO NOTHING";
        query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");

        startTime = System.currentTimeMillis();
        int n = namedParameterJdbcTemplate.update(query, Map.of());
        duration = System.currentTimeMillis() - startTime;
        log.info("addDiffVersionData: inserted {} new records in {} ms", n, duration);

        insertQueryTemplate = "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                "INNER JOIN {tempTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}) " +
                "WHERE {tempTbl}.diff_type IN ('I', 'U')";
        query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");

        startTime = System.currentTimeMillis();
        List<Long> idsForInsert = namedParameterJdbcTemplate.queryForList(query, Map.of(), Long.class);
        duration = System.currentTimeMillis() - startTime;
        log.info("addDiffVersionData: selected {} record IDs in {} ms", idsForInsert.size(), duration);

        insertVersions(idsForInsert, versionId, pgTable);
    }

    @Override
    public void repeatVersion(String tempTable,
                              PgTable pgTable,
                              Integer versionId) {

        Map<String, String> queryPlaceholders = getQueryPlaceholders(tempTable, pgTable);

        //удаляем записи, которых нет в версии
        String deleteQueryTemplate = "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                "INNER JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "LEFT JOIN {tempTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} " +
                "WHERE {refTableVersions}.{versionId} = :version_id AND {tempTbl}.{pk} IS NULL";
        String deleteQuery = StringSubstitutor.replace(deleteQueryTemplate, queryPlaceholders, "{", "}");

        long startTime = System.currentTimeMillis();
        List<Long> idsForDelete = namedParameterJdbcTemplate.queryForList(deleteQuery, Map.of("version_id", versionId), Long.class);
        long duration = System.currentTimeMillis() - startTime;
        log.info("repeatVersion: found {} records to delete in {} ms", idsForDelete.size(), duration);


        //получаем записи, которые есть в temp, но нет в сохраненной версии
        String selectQueryTemplate = "SELECT {tempTbl}.{pk} FROM {tempTbl} " +
                "LEFT JOIN (" +
                "  SELECT {refTbl}.{pk} FROM {refTbl} " +
                "  INNER JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "  WHERE {refTableVersions}.{versionId} = :version_id" +
                ") v ON {tempTbl}.{pk} = v.{pk} " +
                "WHERE v.{pk} IS NULL";
        String selectQuery = StringSubstitutor.replace(selectQueryTemplate, queryPlaceholders, "{", "}");

        startTime = System.currentTimeMillis();
        List<Long> srcIds = namedParameterJdbcTemplate.queryForList(selectQuery, Map.of("version_id", versionId), Long.class);
        duration = System.currentTimeMillis() - startTime;
        log.info("repeatVersion: found {} new records in {} ms", srcIds.size(), duration);


        List<Long> idsForInsert = new ArrayList<>();
        if (!srcIds.isEmpty()) {
            //добавляем запись в refTbl, если ее нет
            String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds)) ON CONFLICT ({uniqColumns}) DO NOTHING RETURNING {syncId}";
            String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");

            startTime = System.currentTimeMillis();
            namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class);
            duration = System.currentTimeMillis() - startTime;
            log.info("repeatVersion: inserted {} new records in {} ms", srcIds.size(), duration);

            //получаем _sync_rec_id
            insertQueryTemplate = "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                    "INNER JOIN {tempTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}) " +
                    "WHERE {tempTbl}.{pk} IN (:srcIds)";
            query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");

            startTime = System.currentTimeMillis();
            idsForInsert = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds", srcIds), Long.class);
            duration = System.currentTimeMillis() - startTime;
            log.info("repeatVersion: selected {} new record IDs in {} ms", idsForInsert.size(), duration);
        }
        //

        //редактируем записи, которые изменились
        //ищем записи, которые есть и в tempTbl и в сохраненной версии (по pk), но с разным hash
        String sqt = "SELECT {tempTbl}.{pk} FROM {tempTbl} " +
                "INNER JOIN {refTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} " +
                "INNER JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "WHERE {refTableVersions}.{versionId} = :version_id " +
                "AND {refTbl}.{hash} <> md5({hashExpression})";
        String sq = StringSubstitutor.replace(sqt, queryPlaceholders, "{", "}");

        startTime = System.currentTimeMillis();
        List<Long> srcIdsFromUpdate = namedParameterJdbcTemplate.queryForList(sq, Map.of("version_id", versionId), Long.class);
        duration = System.currentTimeMillis() - startTime;
        log.info("repeatVersion: found {} modified records in {} ms", srcIdsFromUpdate.size(), duration);

        if (!srcIdsFromUpdate.isEmpty()) {
            String updateQueryTemplate = "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                    "INNER JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                    "WHERE {refTableVersions}.{versionId} = :version_id " +
                    "AND {refTbl}.{pk} IN (:srcIdsFromUpdate)";
            String updateQuery = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders, "{", "}");

            startTime = System.currentTimeMillis();
            List<Long> idsForDeleteFromUpdate = namedParameterJdbcTemplate.queryForList(updateQuery, Map.of("version_id", versionId, "srcIdsFromUpdate", srcIdsFromUpdate), Long.class);
            duration = System.currentTimeMillis() - startTime;
            log.info("repeatVersion: found {} old versions to delete in {} ms", idsForDeleteFromUpdate.size(), duration);
            idsForDelete.addAll(idsForDeleteFromUpdate);

            String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIdsFromUpdate)) ON CONFLICT ({uniqColumns}) DO NOTHING RETURNING {syncId}";
            String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");

            startTime = System.currentTimeMillis();
            namedParameterJdbcTemplate.queryForList(query, Map.of("srcIdsFromUpdate", srcIdsFromUpdate), Long.class);
            duration = System.currentTimeMillis() - startTime;
            log.info("repeatVersion: inserted {} updated records in {} ms", srcIdsFromUpdate.size(), duration);

            //получаем _sync_rec_id
            insertQueryTemplate = "SELECT {refTbl}.{syncId} FROM {refTbl} " +
                    "INNER JOIN {tempTbl} ON {refTbl}.{pk} = {tempTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}) " +
                    "WHERE {tempTbl}.{pk} IN (:srcIdsFromUpdate)";
            query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");

            startTime = System.currentTimeMillis();
            List<Long> idsForInsertFromUpdate = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIdsFromUpdate", srcIdsFromUpdate), Long.class);
            duration = System.currentTimeMillis() - startTime;
            log.info("repeatVersion: selected {} updated record IDs in {} ms", idsForInsertFromUpdate.size(), duration);
            idsForInsert.addAll(idsForInsertFromUpdate);
        }

        //

        insertVersions(idsForInsert, versionId, pgTable);
        deleteVersions(idsForDelete, versionId, pgTable);
    }

    private Map<String, String> getQueryPlaceholders(String tempTable, PgTable pgTable) {
        Assert.isTrue(pgTable.getColumns().isPresent(), "There is no columns in the table " + pgTable.getName());
        Assert.isTrue(pgTable.getPrimaryField().isPresent(), "There is no primary field in the table " + pgTable.getName());
        List<String> fields = pgTable.getColumns().get().stream().map(PgTable.Column::name).collect(Collectors.toList());
        String columnsExpression = String.join(",", fields);
        String hashExpression = getHashExpression(fields, tempTable);
        String pkField = pgTable.getPrimaryField().get();

        // Создаем версию columnsExpression с префиксом временной таблицы для использования в SELECT
        String escapedTempTable = escapeName(tempTable);
        String tempColumnsExpression = fields.stream()
                .map(field -> escapedTempTable + "." + field)
                .collect(Collectors.joining(","));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tempTbl", escapedTempTable);
        placeholders.put("columnsExpression", columnsExpression);
        placeholders.put("tempColumnsExpression", tempColumnsExpression);
        placeholders.put("hashExpression", hashExpression);
        placeholders.put("hash", RECORD_HASH);
        placeholders.put("refTbl", pgTable.getName());
        placeholders.put("pk", pkField);
        placeholders.put("syncId", RECORD_PK_COL);
        placeholders.put("versionId", VERSION_ID);
        placeholders.put("refTableVersions", pgTable.getVersionsTable());
        placeholders.put("uniqColumns", pkField + ", " + RECORD_HASH);

        return placeholders;
    }

    private void deleteVersions(List<Long> ids,
                                Integer versionId,
                                PgTable pgTable) {
        if (ids.isEmpty()) {
            return;
        }

        Map<String, String> queryPlaceholders = Map.of(
                "versionId", VERSION_ID,
                "refTableVersions", pgTable.getVersionsTable());

        // Используем одиночный DELETE с ANY для оптимизации производительности
        String query = StringSubstitutor.replace("DELETE FROM {refTableVersions} " +
                "WHERE record_id = ANY(:ids) AND {versionId} = :version_id", queryPlaceholders, "{", "}");

        Map<String, Object> params = new HashMap<>();
        params.put("ids", ids.toArray(new Long[0]));
        params.put("version_id", versionId);

        long startTime = System.currentTimeMillis();
        int deleted = namedParameterJdbcTemplate.update(query, params);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Deleted {} version records in {} ms", deleted, duration);
    }

    private String getHashExpression(List<String> fields, String table) {
        return fields.stream().map(field -> "coalesce(" + table + "." + field + "::text, '')").collect(joining("||"));
    }

    public void insertVersions(List<Long> ids,
                               Integer versionId,
                               PgTable pgTable) {
        if (ids.isEmpty()) {
            return;
        }

        Map<String, String> queryPlaceholders = Map.of(
                "versionId", VERSION_ID,
                "refTableVersions", pgTable.getVersionsTable());

        String query = StringSubstitutor.replace("INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                "VALUES (:id, :version_id)", queryPlaceholders, "{", "}");

        // Разбиваем на батчи для предотвращения переполнения параметров и памяти
        long startTime = System.currentTimeMillis();
        int totalInserted = 0;

        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, ids.size());
            List<Long> batchIds = ids.subList(i, end);

            SqlParameterSource[] batch = batchIds.stream()
                    .map(id -> new MapSqlParameterSource()
                            .addValue("id", id)
                            .addValue("version_id", versionId))
                    .toArray(SqlParameterSource[]::new);

            namedParameterJdbcTemplate.batchUpdate(query, batch);
            totalInserted += batchIds.size();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Inserted {} version records in {} ms",
                 totalInserted, duration);
    }

    @Override
    public Page<Map<String, Object>> getData(VersionedLocalDataCriteria criteria) {
        Map<String, Object> args = new HashMap<>();
        String name = criteria.getSchemaTable().replace("\"", "");
        name = name + "_versions";
        String sql = String.format("%n  FROM %s %n JOIN %s ON %s._sync_rec_id = %s.record_id WHERE 1=1 %n",
                escapeName(criteria.getSchemaTable()), escapeName(name), escapeName(criteria.getSchemaTable()), escapeName(name));

        if (criteria.getVersion() != null) {
            sql = sql + " AND " + VERSION_ID + ("=(SELECT id from rdm_sync.loaded_version WHERE code = :code AND version = :version)");
            args.put("code", criteria.getRefBookCode());
            args.put("version", criteria.getVersion());
        }


        Page<Map<String, Object>> data = getData0(sql, args, criteria, null);
        data.getContent().forEach(row -> {
            row.remove(RECORD_PK_COL);
            row.remove(VERSION_ID);
            row.remove(RECORD_HASH);
            row.remove("record_id");
            row.remove("id");
        });

        return data;
    }

    private Page<Map<String, Object>> getData0(String sql, Map<String, Object> args, BaseDataCriteria dataCriteria, String selectSubQuery) {

        if (dataCriteria.getFilterSql() != null && !dataCriteria.getFilterSql().isEmpty()) {
            sql += "\n AND " + dataCriteria.getFilterSql();
        } else {
            SqlFilterBuilder filterBuilder = getFiltersClause(dataCriteria.getFilters());
            if (filterBuilder != null) {
                sql += "\n AND " + filterBuilder.build();
                args.putAll(filterBuilder.getParams());
            }
        }

        Integer count = namedParameterJdbcTemplate.queryForObject("SELECT count(*)" + sql, args, Integer.class);
        if (count == null || count == 0)
            return Page.empty();

        int limit = dataCriteria.getLimit();
        if (limit != 1) {
            sql += String.format("%n ORDER BY %s ", addDoubleQuotes(dataCriteria.getPk()));
        }

        sql += String.format("%n LIMIT %d OFFSET %d", limit, dataCriteria.getOffset());

        sql = "SELECT * " + (selectSubQuery != null ? ", " + selectSubQuery + " " : "") + sql;

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
                        } else if (val instanceof Array) {
                            val = Arrays.asList((Object[]) ((Array) val).getArray());
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
}
