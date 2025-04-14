package ru.i_novus.ms.rdm.sync.dao;

import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.dao.builder.SqlFilterBuilder;
import ru.i_novus.ms.rdm.sync.dao.criteria.BaseDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils;

import java.sql.Array;
import java.sql.Timestamp;
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
                                    String refTable,
                                    String pkField,
                                    Integer versionId,
                                    List<String> fields) {
        String columnsExpression = fields.stream().map(RdmSyncDataUtils::escapeName).collect(joining(","));
        String hashExpression = getHashExpression(fields, tempTable);
        String name = refTable.replace("\"", "");
        String versionsTableName = name + "_versions";
        Map<String, String> queryPlaceholders = Map.of("tempTbl", escapeName(tempTable),
                "columnsExpression", columnsExpression,
                "hashExpression", hashExpression,
                "hash", RECORD_HASH,
                "refTbl", escapeName(refTable),
                "pk", escapeName(pkField),
                "syncId", RECORD_PK_COL,
                "versionId", VERSION_ID,
                "refTableVersions", escapeName(versionsTableName));

        Map<String, Object> map = new HashMap<>();
        map.put("version_id", versionId);

        //вставляем записи в refTbl
        String dataQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                "WHERE NOT EXISTS(SELECT 1 FROM {refTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {tempTbl}.{pk} = {refTbl}.{pk}))";
        String query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");
        int n = namedParameterJdbcTemplate.update(query, map);
        log.info("{} rows were inserted into {}.", n, refTable);

        //выбираем из refTbl syncId и вставляем в таблицу версий
        dataQueryTemplate = "WITH table_data AS (" +
                "SELECT {syncId} FROM {refTbl} " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE {refTbl}.{hash} = md5({hashExpression}) AND {pk} = {refTbl}.{pk})" +
                ") " +
                "INSERT INTO {refTableVersions} (record_id, {versionId}) " +
                "SELECT {syncId}, :version_id " +
                "FROM table_data";
        query = StringSubstitutor.replace(dataQueryTemplate, queryPlaceholders, "{", "}");
        int m = namedParameterJdbcTemplate.update(query, map);
        log.info("{} rows were inserted into {}.", m, versionsTableName);
    }

    @Override
    public void addDiffVersionData(String tempTable,
                                   String refTable,
                                   String pkField,
                                   String code,
                                   Integer versionId,
                                   List<String> fields,
                                   String syncedVersion) {

        log.info("Begin add diff version for {} version {}", refTable, versionId);

        String columnsExpression = fields.stream().map(RdmSyncDataUtils::escapeName).collect(joining(","));
        String hashExpression = getHashExpression(fields, tempTable);
        String name = refTable.replace("\"", "");
        String versionsTableName = name + "_versions";
        Map<String, String> queryPlaceholders = Map.of("tempTbl", escapeName(tempTable),
                "columnsExpression", columnsExpression,
                "hashExpression", hashExpression,
                "hash", RECORD_HASH,
                "refTbl", escapeName(refTable),
                "pk", escapeName(pkField),
                "syncId", RECORD_PK_COL,
                "versionId", VERSION_ID,
                "refTableVersions", escapeName(versionsTableName),
                "uniqColumns", escapeName(pkField) + ", " + escapeName(RECORD_HASH));

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

        insertVersions(idsForInsert, versionId, refTable);

        log.info("End add diff version for {} version {}", refTable, versionId);
    }

    @Override
    public void repeatVersion(String tempTable,
                              String refTable,
                              String pkField,
                              Integer versionId,
                              List<String> fields) {
        log.info("Begin repeat version for {} version {}", refTable, versionId);

        String columnsExpression = fields.stream().map(RdmSyncDataUtils::escapeName).collect(joining(","));
        String hashExpression = getHashExpression(fields, tempTable);
        String name = refTable.replace("\"", "");
        String versionsTableName = name + "_versions";
        Map<String, String> queryPlaceholders = Map.ofEntries(
                Map.entry("tempTbl", escapeName(tempTable)),
                Map.entry("columnsExpression", columnsExpression),
                Map.entry("hashExpression", hashExpression),
                Map.entry("hash", escapeName(RECORD_HASH)),
                Map.entry("refTbl", escapeName(refTable)),
                Map.entry("pk", escapeName(pkField)),
                Map.entry("syncId", escapeName(RECORD_PK_COL)),
                Map.entry("refTableVersions", escapeName(versionsTableName)),
                Map.entry("versionId", VERSION_ID),
                Map.entry("uniqColumns", escapeName(pkField) + ", " + escapeName(RECORD_HASH)));

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
        List<Long> srcIds2 = namedParameterJdbcTemplate.queryForList(sq, Map.of("version_id", versionId), Long.class);

        if (!srcIds2.isEmpty()) {
            String updateQueryTemplate = "SELECT {syncId} FROM {refTbl} JOIN {refTableVersions} ON {refTbl}.{syncId} = {refTableVersions}.record_id " +
                    "WHERE {refTableVersions}.{versionId} = :version_id " +
                    "AND {pk} in (:srcIds2)";
            String updateQuery = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders, "{", "}");
            List<Long> idsForDelete2 = namedParameterJdbcTemplate.queryForList(updateQuery, Map.of("version_id", versionId, "srcIds2", srcIds2), Long.class);
            idsForDelete.addAll(idsForDelete2);
            log.info("idsForDelete2: {}", idsForDelete2);

            String insertQueryTemplate = "INSERT INTO {refTbl} ({columnsExpression}, {hash}) (SELECT {columnsExpression}, md5({hashExpression}) FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds2)) ON CONFLICT ({uniqColumns}) DO NOTHING RETURNING {syncId}";
            String query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds2", srcIds2), Long.class);


            //получаем _sync_rec_id
            insertQueryTemplate = "SELECT {syncId} FROM {refTbl} WHERE EXISTS (SELECT 1 FROM {tempTbl} " +
                    "WHERE {pk} in (:srcIds2) AND {pk} = {refTbl}.{pk} AND {refTbl}.{hash} = md5({hashExpression}))";
            query = StringSubstitutor.replace(insertQueryTemplate, queryPlaceholders, "{", "}");
            List<Long> idsForInsert2 = namedParameterJdbcTemplate.queryForList(query, Map.of("srcIds2", srcIds2), Long.class);
            idsForInsert.addAll(idsForInsert2);
            log.info("idsForInsert2: {}", idsForInsert2);
        }

        //

        insertVersions(idsForInsert, versionId, refTable);
        deleteIntervals(idsForDelete, versionId, refTable);

        log.info("End repeat version for {} version {}", refTable, versionId);
    }

    private void deleteIntervals(List<Long> ids,
                                 Integer versionId,
                                 String refTable) {

        Map<String, Object> params = new HashMap<>();
        params.put("version_id", versionId);

        String name = refTable.replace("\"", "");
        String versionsTableName = name + "_versions";
        Map<String, String> queryPlaceholders = Map.of(
                "versionId", VERSION_ID,
                "refTableVersions", escapeName(versionsTableName));

        ids.forEach(id -> {
            params.put("id", id);
            namedParameterJdbcTemplate.update(StringSubstitutor.replace("DELETE FROM {refTableVersions} " +
                            "WHERE record_id = :id AND {versionId} = :version_id", queryPlaceholders, "{", "}"), params);
        });
    }

    private String getHashExpression(List<String> fields, String table) {
        return fields.stream().map(field -> "coalesce(" + escapeName(table) + "." + escapeName(field) + "::text, '')").collect(joining("||"));
    }

    public void insertVersions(List<Long> ids,
                               Integer versionId,
                               String refTable) {
        Map<String, Object> params = new HashMap<>();
        params.put("version_id", versionId);
        ids.forEach(id -> {
            params.put("id", id);
            List<Long> dateIds = namedParameterJdbcTemplate.queryForList("INSERT INTO " + escapeName(refTable + "_versions") + "(record_id, " + VERSION_ID + ") " +
                    "VALUES (:id, :version_id) RETURNING id", params, Long.class);
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
                        } else if (val instanceof Array) {
                            val = Arrays.asList((Object[]) ((Array) val).getArray());
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
}
