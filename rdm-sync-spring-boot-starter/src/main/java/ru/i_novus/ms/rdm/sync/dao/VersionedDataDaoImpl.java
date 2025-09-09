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
        String dataQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE NOT EXISTS(SELECT 1 FROM {refTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {tempTbl}.{pk} = {refTbl}.{pk}))";
        String query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");
        namedParameterJdbcTemplate.update(query, map);

        //выбираем из refTbl syncId и вставляем в таблицу версий
        dataQueryTemplate = "WITH table_data AS (" +
                "SELECT {syncId} FROM {refTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {pk} = {refTbl}.{pk})" +
                ") " +
                "INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                "SELECT {syncId}, :version_id " +
                "FROM table_data";
        query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");
        namedParameterJdbcTemplate.update(query, map);
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
                "SELECT {syncId} FROM {refTbl} JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "WHERE {refTableVersions}.{versionId} = :prev_version_id " +
                "AND NOT EXISTS(SELECT 1 FROM {tempTbl} WHERE {pk} = {refTbl}.{pk})" +
                ")" +
                "INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                "SELECT {syncId}, :version_id " +
                "FROM table_data";

        String query = StringSubstitutor.replace(str, queryPlaceholders, "{", "}");
        Map<String, Object> map = new HashMap<>();
        map.put("version_id", versionId);
        map.put("prev_version_id", loadedVersion.getId());
        namedParameterJdbcTemplate.update(query, map);

        //обработка измененных записей(добавление и редактирование)
        String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE (diff_type = 'I' OR diff_type = 'U')) ON CONFLICT ({uniqColumns}) DO NOTHING";
        query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
        namedParameterJdbcTemplate.update(query, Map.of());

        insertQueryTemplate = "SELECT {syncId} FROM {refTbl} WHERE EXISTS (SELECT 1 FROM {tempTbl} " +
                "WHERE (diff_type = 'I' OR diff_type = 'U') AND {pk} = {refTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}))";
        query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> idsForInsert = namedParameterJdbcTemplate.queryForList(query, Map.of(), Long.class);
        log.info("idsForInsert: {}", idsForInsert);

        insertVersions(idsForInsert, versionId, pgTable);
    }

    @Override
    public void repeatVersion(String tempTable,
                              PgTable pgTable,
                              Integer versionId) {

        Map<String, String> queryPlaceholders = getQueryPlaceholders(tempTable, pgTable);

        //удаляем записи, которых нет в версии
        String deleteQueryTemplate = "SELECT {syncId} FROM {refTbl} JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                " WHERE {refTableVersions}.{versionId} = :version_id " +
                " AND NOT EXISTS(SELECT 1 FROM {tempTbl} WHERE {pk} = {refTbl}.{pk})";
        String deleteQuery = StringSubstitutor.replace(deleteQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> idsForDelete = namedParameterJdbcTemplate.queryForList(deleteQuery, Map.of("version_id", versionId), Long.class);
        log.info("idsForDelete: {}", idsForDelete);
        //


        //получаем записи, которые есть в temp, но нет в сохраненной версии
        String selectQueryTemplate = "SELECT {pk} FROM {tempTbl} " +
                "WHERE NOT EXISTS(SELECT 1 FROM {refTbl} JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "WHERE {refTableVersions}.{versionId} = :version_id " +
                "AND {pk} = {tempTbl}.{pk})";
        String selectQuery = StringSubstitutor.replace(selectQueryTemplate, queryPlaceholders, "{", "}");
        List<Long> srcIds = namedParameterJdbcTemplate.queryForList(selectQuery, Map.of("version_id", versionId), Long.class);


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
            log.info("idsForInsert: {}", idsForInsert);
        }
        //

        //редактируем записи, которые изменились
        //ищем записи, которые есть и в tempTbl и в сохраненной версии (по pk), но с разным hash
        String sqt = "SELECT {pk} FROM {tempTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {refTbl} JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                "WHERE {refTableVersions}.{versionId} = :version_id " +
                "AND {refTbl}.{pk} = {tempTbl}.{pk} " +
                "AND {refTbl}.{hash} <> md5({hashExpression}))";
        String sq = StringSubstitutor.replace(sqt, queryPlaceholders, "{", "}");
        List<Long> srcIdsFromUpdate = namedParameterJdbcTemplate.queryForList(sq, Map.of("version_id", versionId), Long.class);

        if (!srcIdsFromUpdate.isEmpty()) {
            String updateQueryTemplate = "SELECT {syncId} FROM {refTbl} JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                    "WHERE {refTableVersions}.{versionId} = :version_id " +
                    "AND {pk} in (:srcIdsFromUpdate)";
            String updateQuery = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders, "{", "}");
            List<Long> idsForDeleteFromUpdate = namedParameterJdbcTemplate.queryForList(updateQuery, Map.of("version_id", versionId, "srcIdsFromUpdate", srcIdsFromUpdate), Long.class);
            idsForDelete.addAll(idsForDeleteFromUpdate);
            log.info("idsForDeleteFromUpdate: {}", idsForDeleteFromUpdate);

            String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIdsFromUpdate)) ON CONFLICT ({uniqColumns}) DO NOTHING RETURNING {syncId}";
            String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            namedParameterJdbcTemplate.queryForList(query, Map.of("srcIdsFromUpdate", srcIdsFromUpdate), Long.class);


            //получаем _sync_rec_id
            insertQueryTemplate = "SELECT {syncId} FROM {refTbl} WHERE EXISTS (SELECT 1 FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIdsFromUpdate) AND {pk} = {refTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}))";
            query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            List<Long> idsForInsertFromUpdate = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIdsFromUpdate", srcIdsFromUpdate), Long.class);
            idsForInsert.addAll(idsForInsertFromUpdate);
            log.info("idsForInsertFromUpdate: {}", idsForInsertFromUpdate);
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

        return Map.of("tempTbl", escapeName(tempTable),
                "columnsExpression", columnsExpression,
                "hashExpression", hashExpression,
                "hash", RECORD_HASH,
                "refTbl", pgTable.getName(),
                "pk", pkField,
                "syncId", RECORD_PK_COL,
                "versionId", VERSION_ID,
                "refTableVersions", pgTable.getVersionsTable(),
                "uniqColumns", pkField + ", " + RECORD_HASH);
    }

    private void deleteVersions(List<Long> ids,
                                Integer versionId,
                                PgTable pgTable) {

        Map<String, Object> params = new HashMap<>();
        params.put("version_id", versionId);

        Map<String, String> queryPlaceholders = Map.of(
                "versionId", VERSION_ID,
                "refTableVersions", pgTable.getVersionsTable());

        ids.forEach(id -> {
            params.put("id", id);
            namedParameterJdbcTemplate.update(StringSubstitutor.replace("DELETE FROM {refTableVersions} " +
                            "WHERE record_id = :id AND {versionId} = :version_id", queryPlaceholders, "{", "}"), params);
        });
    }

    private String getHashExpression(List<String> fields, String table) {
        return fields.stream().map(field -> "coalesce(" + table + "." + field + "::text, '')").collect(joining("||"));
    }

    public void insertVersions(List<Long> ids,
                               Integer versionId,
                               PgTable pgTable) {
        Map<String, Object> params = new HashMap<>();
        params.put("version_id", versionId);

        Map<String, String> queryPlaceholders = Map.of(
                "versionId", VERSION_ID,
                "refTableVersions", pgTable.getVersionsTable());

        ids.forEach(id -> {
            params.put("id", id);
            namedParameterJdbcTemplate.update(StringSubstitutor.replace("INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                    "VALUES (:id, :version_id)", queryPlaceholders, "{", "}"), params);
        });
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
            row.remove("rdm_sync_internal_local_row_state");//нужно ли удалять?
        });

        return data;
    }

    private Page<Map<String, Object>> getData0(String sql, Map<String, Object> args, BaseDataCriteria dataCriteria, String selectSubQuery) {

        if (dataCriteria.getFilterSql() != null) {
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
