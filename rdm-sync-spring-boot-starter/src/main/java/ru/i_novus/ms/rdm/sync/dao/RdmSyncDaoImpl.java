package ru.i_novus.ms.rdm.sync.dao;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.builder.SqlFilterBuilder;
import ru.i_novus.ms.rdm.sync.dao.criteria.BaseDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addDoubleQuotes;
import static ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState.*;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public class RdmSyncDaoImpl implements RdmSyncDao {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncDaoImpl.class);

    private static final String INTERNAL_FUNCTION = "rdm_sync_internal_update_local_row_state()";

    private static final String LOCAL_ROW_STATE_UPDATE_FUNC = "CREATE OR REPLACE FUNCTION %1$s\n" +
            "  RETURNS trigger AS \n" +
            "$BODY$ \n" +
            "  BEGIN \n" +
            "    NEW.%2$s='%3$s'; \n" +
            "    RETURN NEW; \n" +
            "  END; \n" +
            "$BODY$ LANGUAGE 'plpgsql' \n";

    private static final String RECORD_SYS_COL = "_sync_rec_id";
    private static final String RECORD_SYS_COL_INFO = "bigserial PRIMARY KEY";
    private static final String VERSIONS_SYS_COL = "_versions";
    private static final String LOADED_VERSION_REF = "version_id";
    private static final String HASH_SYS_COL = "_hash";

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<VersionMapping> getVersionMappings() {

        final String sql = "SELECT m.id, code, name, version, \n" +
                "       sys_table, sys_pk_field, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type, match_case, refreshable_range \n" +
                "  FROM rdm_sync.version v \n" +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id \n" +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id \n";

        return namedParameterJdbcTemplate.query(sql,
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getString(8),
                        rs.getString(9),
                        toLocalDateTime(rs, 10, LocalDateTime.MIN),
                        rs.getInt(11),
                        rs.getInt(12),
                        SyncTypeEnum.valueOf(rs.getString(13)),
                        new Range(rs.getString(4)),
                        rs.getBoolean(14),
                        rs.getBoolean(15)
                )
        );
    }

    @Override
    public LoadedVersion getLoadedVersion(String code, String version) {
        List<LoadedVersion> result = namedParameterJdbcTemplate.query("select * from rdm_sync.loaded_version where code = :code and version = :version", Map.of("code", code, "version", version),
                (ResultSet rs, int rowNum) -> new LoadedVersion(rs.getInt("id"), rs.getString("code"), rs.getString("version"), rs.getTimestamp("publication_dt").toLocalDateTime(), toLocalDateTime(rs.getTimestamp("close_dt")), rs.getTimestamp("load_dt").toLocalDateTime(), rs.getBoolean("is_actual"))
        );

        if (CollectionUtils.isEmpty(result)) {
            return null;
        }

        return result.get(0);
    }

    @Override
    public List<LoadedVersion> getLoadedVersions(String code) {
        return namedParameterJdbcTemplate.query("select * from rdm_sync.loaded_version where code = :code ", Map.of("code", code),
                (ResultSet rs, int rowNum) -> new LoadedVersion(rs.getInt("id"), rs.getString("code"), rs.getString("version"), rs.getTimestamp("publication_dt").toLocalDateTime(), toLocalDateTime(rs.getTimestamp("close_dt")),rs.getTimestamp("load_dt").toLocalDateTime(), rs.getBoolean("is_actual")));
    }

    @Override
    public LoadedVersion getActualLoadedVersion(String code) {
        List<LoadedVersion> result = namedParameterJdbcTemplate.query("select * from rdm_sync.loaded_version where code = :code and is_actual = true", Map.of("code", code),
                (ResultSet rs, int rowNum) -> new LoadedVersion(rs.getInt("id"), rs.getString("code"), rs.getString("version"), rs.getTimestamp("publication_dt").toLocalDateTime(), toLocalDateTime(rs.getTimestamp("close_dt")), rs.getTimestamp("load_dt").toLocalDateTime(), rs.getBoolean("is_actual"))
        );
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        return result.get(0);
    }

    @Override
    public boolean existsLoadedVersion(String code) {
        return namedParameterJdbcTemplate
                .queryForObject("select exists(select 1 from rdm_sync.loaded_version where code = :code)",
                        Map.of("code", code), Boolean.class);
    }

    @Override
    public List<FieldMapping> getFieldMappings(String refbookCode) {

        final String sql = "SELECT m.sys_field, m.sys_data_type, m.rdm_field, m.ignore_if_not_exists, m.default_value \n" +
                "  FROM rdm_sync.field_mapping m \n" +
                " WHERE m.mapping_id = ( \n" +
                "       SELECT v.mapping_id \n" +
                "         FROM rdm_sync.version v \n" +
                "        WHERE v.ref_id = ( \n" +
                "              SELECT r.id FROM rdm_sync.refbook r WHERE r.code = :code \n" +
                "              )" +
                "       ) \n";

        return namedParameterJdbcTemplate.query(sql,
                (rs, rowNum) -> new FieldMapping(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4),
                        rs.getString(5)
                )
        );
    }

    @Override
    public List<FieldMapping> getFieldMappings(Integer mappingId) {
        final String sql = "SELECT m.sys_field, m.sys_data_type, m.rdm_field, m.ignore_if_not_exists, m.default_value \n" +
                "  FROM rdm_sync.field_mapping m \n" +
                " WHERE m.mapping_id = :mappingId";

        return namedParameterJdbcTemplate.query(sql,
                Map.of("mappingId", mappingId),
                (rs, rowNum) -> new FieldMapping(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4),
                        rs.getString(5)
                )
        );
    }

    @Override
    public List<Pair<String, String>> getLocalColumnTypes(String schemaTable) {

        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];

        String sql = "SELECT column_name, data_type \n" +
                "  FROM information_schema.columns \n" +
                " WHERE table_schema = :schemaName \n" +
                "   AND table_name = :tableName \n" +
                "   AND column_name != :internal_local_row_state_column";

        List<Pair<String, String>> list = namedParameterJdbcTemplate.query(sql,
                Map.of("schemaName", schema,
                        "tableName", table,
                        "internal_local_row_state_column", RDM_SYNC_INTERNAL_STATE_COLUMN),
                (rs, rowNum) -> Pair.of(rs.getString(1), rs.getString(2))
        );

        if (list.isEmpty())
            throw new RdmException("No table '" + table + "' in schema '" + schema + "'.");

        return list;
    }

    @Override
    public boolean isIdExists(String schemaTable, String primaryField, Object primaryValue) {

        final String sql = String.format("SELECT count(*) > 0 FROM %s WHERE %s = :primary",
                escapeName(schemaTable), addDoubleQuotes(primaryField));

        final Boolean result = namedParameterJdbcTemplate.queryForObject(sql,
                Map.of("primary", primaryValue),
                Boolean.class
        );
        return Boolean.TRUE.equals(result);
    }

    @Override
    public Integer insertLoadedVersion(String code, String version, LocalDateTime publishDate, LocalDateTime closeDate, boolean actual) {
        final String sql = "INSERT INTO rdm_sync.loaded_version(code, version, publication_dt, close_dt, load_dt, is_actual) \n" +
                "   VALUES(:code, :version, :publishDate, :closeDate, :updateDate, :actual) RETURNING id";

        Map<String, Object> params = new HashMap<>();
        params.put("version", version);
        params.put("publishDate", publishDate);
        params.put("closeDate", closeDate);
        params.put("updateDate", LocalDateTime.now(Clock.systemUTC()));
        params.put("code", code);
        params.put("actual", actual);
        return namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
    }

    @Override
    public void updateLoadedVersion(Integer id, String version, LocalDateTime publishDate, LocalDateTime closeDate) {

        final String sql = "UPDATE rdm_sync.loaded_version \n" +
                "   SET version = :version, \n" +
                "       publication_dt = :publication_dt, \n" +
                "       close_dt = :close_dt, \n" +
                "       load_dt = :load_dt \n" +
                " WHERE id = :id";

        Map<String, Object> params = new HashMap<>();
        params.put("version", version);
        params.put("publication_dt", publishDate);
        params.put("close_dt", closeDate);
        params.put("load_dt", LocalDateTime.now(Clock.systemUTC()));
        params.put("id", id);
        namedParameterJdbcTemplate.update(sql, params);
    }


    @Override
    public void closeLoadedVersion(String code, String version, LocalDateTime closeDate) {
        final String sql = "UPDATE rdm_sync.loaded_version \n" +
                "   SET close_dt = :closeDate, is_actual=false \n" +
                " WHERE version = :version and code = :code";

        namedParameterJdbcTemplate.update(sql,
                Map.of("version", version,
                        "closeDate", closeDate,
                        "code", code)
        );
    }

    @Override
    public void insertRow(String schemaTable, Map<String, Object> row, boolean markSynced) {

        insertRows(schemaTable, List.of(row), markSynced);
    }

    @Override
    public void insertRows(String schemaTable, List<Map<String, Object>> rows, boolean markSynced) {
        List<Map<String, Object>> newRows = rows.stream().map(map -> {
            Map<String, Object> newMap = new HashMap<>(map);
            newMap.put(RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name());
            return newMap;
        }).collect(toList());
        insertRows(schemaTable, newRows);
    }

    @Override
    public void insertVersionedRows(String schemaTable, List<Map<String, Object>> rows, String version) {
        insertRows(schemaTable, convertToVersionedRows(rows, version));
    }

    @Override
    public void insertSimpleVersionedRows(String schemaTable, List<Map<String, Object>> rows, Integer loadedVersionId) {

        insertRows(schemaTable, convertToSimpleVersionedRows(rows, loadedVersionId));

    }

    @Override
    public void updateRow(String schemaTable, String primaryField, Map<String, Object> row, boolean markSynced) {

        if (markSynced) {
            row = new HashMap<>(row);
            row.put(RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name());
        }

        executeUpdate(schemaTable, Collections.singletonList(row), primaryField);
    }

    @Override
    public void updateRows(String schemaTable, String primaryField, List<Map<String, Object>> rows, boolean markSynced) {
        executeUpdate(schemaTable, rows, primaryField);
    }

    @Override
    public void markDeleted(String schemaTable, String primaryField, String isDeletedField,
                            Object primaryValue, LocalDateTime deletedTime, boolean markSynced) {

        Map<String, Object> args = new HashMap<>();
        args.put(primaryField, primaryValue);
        args.put(isDeletedField, deletedTime);
        if (markSynced)
            args.put(RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name());

        executeUpdate(schemaTable, Collections.singletonList(args), primaryField);
    }

    private List<Map<String, Object>> convertToVersionedRows(List<Map<String, Object>> rows, String version) {
        return rows.stream().map(row -> {
            Map<String, Object> newMap = new HashMap<>(row);
            StringBuilder stringBuilder = new StringBuilder();
            row.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
            newMap.put(HASH_SYS_COL, DigestUtils.md5DigestAsHex(stringBuilder.toString().getBytes(StandardCharsets.UTF_8)));
            newMap.put(VERSIONS_SYS_COL, "{" + version + "}");
            return newMap;
        }).collect(toList());
    }

    private List<Map<String, Object>> convertToSimpleVersionedRows(List<Map<String, Object>> rows, Integer loadedVersionId) {
        return rows.stream().map(row -> {
            Map<String, Object> newMap = new HashMap<>(row);
            newMap.put(LOADED_VERSION_REF, loadedVersionId);
            return newMap;
        }).collect(toList());
    }

    private void insertRows(String schemaTable, List<Map<String, Object>> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }

        StringJoiner columns = new StringJoiner(",");
        StringJoiner values = new StringJoiner(",");
        Map<String, Object>[] batchValues = new Map[rows.size()];
        concatColumnsAndValues(columns, values, batchValues, rows);

        final String sql = String.format("INSERT INTO %s (%s) VALUES(%s)",
                escapeName(schemaTable), columns, values);

        namedParameterJdbcTemplate.batchUpdate(sql, batchValues);
    }


    private void executeUpdate(String table, List<Map<String, Object>> rows, String primaryField) {

        if (CollectionUtils.isEmpty(rows)) {
            return;
        }

        String sqlFormat = "UPDATE %s SET %s";
        if (primaryField != null) {
            sqlFormat += " WHERE %s = :%s";
        }

        final String fields = rows.get(0).keySet().stream()
                .filter(field -> !field.equals(primaryField))
                .map(field -> addDoubleQuotes(field) + " = :" + field)
                .collect(joining(", "));

        String sql = String.format(sqlFormat, escapeName(table), fields, addDoubleQuotes(primaryField), primaryField);

        Map<String, Object>[] batchValues = new Map[rows.size()];
        namedParameterJdbcTemplate.batchUpdate(sql, rows.toArray(batchValues));
    }

    @Override
    public void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack) {

        final String sql = "INSERT INTO rdm_sync.log \n" +
                "      (code, current_version, new_version, status, date, message, stack) \n" +
                "VALUES(?,?,?,?,?,?,?)";

        getJdbcTemplate().update(sql,
                refbookCode, oldVersion, newVersion, status, new java.util.Date(), message, stack
        );
    }

    @Override
    public List<Log> getList(LocalDate date, String refbookCode) {

        LocalDate end = date.plusDays(1);

        List<Object> args = new ArrayList<>();
        args.add(date);
        args.add(end);
        if (refbookCode != null) {
            args.add(refbookCode);
        }

        final String sqlFormat = "SELECT id, code, current_version, new_version, \n" +
                "       status, date, message, stack \n" +
                "  FROM rdm_sync.log \n" +
                " WHERE date >= ? \n" +
                "   AND date < ? \n" +
                "%s";
        final String sql = String.format(sqlFormat, refbookCode != null ? "   AND code = ?" : "");

        return getJdbcTemplate().query(sql,
                (rs, rowNum) -> new Log(rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getTimestamp(6).toLocalDateTime(),
                        rs.getString(7),
                        rs.getString(8)),
                args.toArray()
        );
    }


    @Override
    @Transactional
    public Integer insertVersionMapping(VersionMapping versionMapping) {

        final String insMappingSql = "insert into rdm_sync.mapping (\n" +
                "    deleted_field,\n" +
                "    mapping_version,\n" +
                "    sys_table,\n" +
                "    sys_pk_field,\n" +
                "    unique_sys_field," +
            "        match_case," +
                "    refreshable_range)\n" +
                "values (\n" +
                "    :deleted_field,\n" +
                "    :mapping_version,\n" +
                "    :sys_table,\n" +
                "    :sys_pk_field,\n" +
                "    :unique_sys_field," +
                "    :match_case," +
                "    :refreshable_range) RETURNING id";

        Integer mappingId = namedParameterJdbcTemplate.queryForObject(insMappingSql, toInsertMappingValues(versionMapping), Integer.class);

        SyncRefBook syncRefBook = getSyncRefBook(versionMapping.getCode());
        Integer refBookId;
        if(syncRefBook == null) {
            final String insRefSql = "insert into rdm_sync.refbook(code, name, source_id, sync_type, range) values(:code, :name, (SELECT id FROM rdm_sync.source WHERE code=:source_code), :type, :range)  RETURNING id";
            String refBookName = versionMapping.getRefBookName();
            Map<String, String> params = new HashMap<>(Map.of("code", versionMapping.getCode(), "name", refBookName != null ? refBookName : versionMapping.getCode(), "source_code", versionMapping.getSource(), "type", versionMapping.getType().name()));
            params.put("range", versionMapping.getRange().getRange());
            refBookId = namedParameterJdbcTemplate.queryForObject(insRefSql,
                    params,
                    Integer.class);
        } else {
            refBookId = syncRefBook.getId();
        }

        namedParameterJdbcTemplate.update("insert into rdm_sync.version(ref_id, mapping_id, version) values(:refId, :mappingId, :version)",
                Map.of("refId", refBookId, "mappingId", mappingId, "version", versionMapping.getRange().getRange() == null ? "" : versionMapping.getRange().getRange()));

        return mappingId;
    }

    private Map<String, Object> toInsertMappingValues(VersionMapping versionMapping) {

        Map<String, Object> result = new HashMap<>(5);
        result.put("mapping_version", versionMapping.getMappingVersion());
        result.put("sys_table", versionMapping.getTable());
        result.put("sys_pk_field", versionMapping.getSysPkColumn());
        result.put("unique_sys_field", versionMapping.getPrimaryField());
        result.put("deleted_field", versionMapping.getDeletedField());
        result.put("match_case", versionMapping.isMatchCase());
        result.put("refreshable_range", versionMapping.isRefreshableRange());

        return result;
    }

    @Override
    public void updateCurrentMapping(VersionMapping versionMapping) {

        final String sql = "update rdm_sync.mapping set deleted_field = :deleted_field, mapping_version = :mapping_version, sys_table = :sys_table, unique_sys_field = :unique_sys_field, match_case = :match_case, refreshable_range = :refreshable_range" +
                " where id = (select mapping_id from rdm_sync.version where version = :version and ref_id = (select id from rdm_sync.refbook where code = :code))";

        namedParameterJdbcTemplate.update(sql, toUpdateMappingValues(versionMapping));

        final String updateRefbook = "update rdm_sync.refbook set " +
                "(name, source_id, sync_type, range) = " +
                "(:name, (select id from rdm_sync.source where code = :source_code), :sync_type, :range) " +
                " where code = :code";
        Map<String, Object> updateParams = new HashMap<>(
                Map.of("code", versionMapping.getCode(),
                "source_code", versionMapping.getSource(),
                "sync_type", versionMapping.getType().toString(),
                "name", versionMapping.getRefBookName()));
        updateParams.put("range", versionMapping.getRange().getRange());
        namedParameterJdbcTemplate.update(updateRefbook, updateParams);
    }

    private Map<String, Object> toUpdateMappingValues(VersionMapping versionMapping) {

        Map<String, Object> result = new HashMap<>(6);
        result.put("code", versionMapping.getCode());
        result.put("version", versionMapping.getRange().getRange());
        result.put("mapping_version", versionMapping.getMappingVersion());
        result.put("sys_table", versionMapping.getTable());
        result.put("unique_sys_field", versionMapping.getPrimaryField());
        result.put("deleted_field", versionMapping.getDeletedField());
        result.put("match_case", versionMapping.isMatchCase());
        result.put("refreshable_range", versionMapping.isRefreshableRange());

        return result;
    }

    @Override
    public void insertFieldMapping(Integer mappingId, List<FieldMapping> fieldMappings) {

        final String sqlDelete = "DELETE FROM rdm_sync.field_mapping WHERE mapping_id = ?";

        getJdbcTemplate().update(sqlDelete, mappingId);

        final String sqlInsert = "INSERT INTO rdm_sync.field_mapping \n" +
                "      (mapping_id, sys_field, sys_data_type, rdm_field, ignore_if_not_exists, default_value) \n" +
                "VALUES(?, ?, ?, ?, ?, ?)";

        getJdbcTemplate().batchUpdate(sqlInsert,
                new BatchPreparedStatementSetter() {

                    public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {

                        ps.setInt(1, mappingId);
                        ps.setString(2, fieldMappings.get(i).getSysField());
                        ps.setString(3, fieldMappings.get(i).getSysDataType());
                        ps.setString(4, fieldMappings.get(i).getRdmField());
                        ps.setBoolean(5, fieldMappings.get(i).getIgnoreIfNotExists());
                        ps.setString(6, fieldMappings.get(i).getDefaultValue());
                    }

                    public int getBatchSize() {
                        return fieldMappings.size();
                    }

                });
    }

    @Override
    public boolean lockRefBookForUpdate(String code, boolean blocking) {

        logger.info("lock {} for update", code);
        String sql = "SELECT 1 FROM rdm_sync.refbook WHERE code = :code FOR UPDATE";
        if (!blocking)
            sql += " NOWAIT";

        try {
            namedParameterJdbcTemplate.queryForObject(sql, Map.of("code", code), Integer.class);
            logger.info("Lock for refbook {} successfully acquired.", code);

            return true;

        } catch (CannotAcquireLockException ex) {
            logger.info("Lock for refbook {} cannot be acquired.", code, ex);
            return false;
        }
    }

    @Override
    public void addInternalLocalRowStateUpdateTrigger(String schema, String table) {

        String triggerName = escapeName(getInternalLocalStateUpdateTriggerName(schema, table));
        String schemaTable = escapeName(schema + "." + table);

        final String sqlExists = "SELECT EXISTS(SELECT 1 FROM pg_trigger WHERE NOT tgisinternal AND tgname = :tgname)";
        Boolean exists = namedParameterJdbcTemplate.queryForObject(sqlExists, Map.of("tgname", triggerName.replaceAll("\"", "")), Boolean.class);

        if (Boolean.TRUE.equals(exists))
            return;

        final String sqlCreateFormat = "CREATE TRIGGER %s \n" +
                "  BEFORE INSERT OR UPDATE \n" +
                "  ON %s \n" +
                "  FOR EACH ROW \n" +
                "  EXECUTE PROCEDURE %s;";
        String sqlCreate = String.format(sqlCreateFormat, triggerName, schemaTable, INTERNAL_FUNCTION);
        getJdbcTemplate().execute(sqlCreate);
    }

    @Override
    public void createOrReplaceLocalRowStateUpdateFunction() {

        String sql = String.format(LOCAL_ROW_STATE_UPDATE_FUNC, INTERNAL_FUNCTION, RDM_SYNC_INTERNAL_STATE_COLUMN, DIRTY);

        getJdbcTemplate().execute(sql);
    }

    @Override
    public void addInternalLocalRowStateColumnIfNotExists(String schema, String table) {

        String schemaTable = escapeName(schema + "." + table);
        Boolean exists = namedParameterJdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = :schema AND table_name = :table AND column_name = :internal_state_column)", Map.of("schema", schema, "table", table, "internal_state_column", RDM_SYNC_INTERNAL_STATE_COLUMN), Boolean.class);
        if (Boolean.TRUE.equals(exists))
            return;

        String query = String.format("ALTER TABLE %s ADD COLUMN %s VARCHAR NOT NULL DEFAULT '%s'", schemaTable, RDM_SYNC_INTERNAL_STATE_COLUMN, DIRTY);
        getJdbcTemplate().execute(query);

        query = String.format("CREATE INDEX ON %s (%s)", schemaTable, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN));
        getJdbcTemplate().execute(query);

        int n = namedParameterJdbcTemplate.update(String.format("UPDATE %s SET %s = :synced", schemaTable, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN)), Map.of("synced", SYNCED.name()));
        if (n != 0)
            logger.info("{} records updated internal state to {} in table {}", n, SYNCED, schemaTable);
    }

    @Override
    public void disableInternalLocalRowStateUpdateTrigger(String table) {

        String[] split = table.split("\\.");
        String triggerName = escapeName(getInternalLocalStateUpdateTriggerName(split[0], split[1]));
        String query = String.format("ALTER TABLE %s DISABLE TRIGGER %s", escapeName(table), triggerName);

        getJdbcTemplate().execute(query);
    }

    @Override
    public void enableInternalLocalRowStateUpdateTrigger(String table) {

        String[] split = table.split("\\.");
        String triggerName = escapeName(getInternalLocalStateUpdateTriggerName(split[0], split[1]));
        String query = String.format("ALTER TABLE %s ENABLE TRIGGER %s", escapeName(table), triggerName);

        getJdbcTemplate().execute(query);
    }

    @Override
    public boolean existsInternalLocalRowStateUpdateTrigger(String table) {
        String[] split = table.split("\\.");
        String sql = "select exists(select 1 from pg_trigger where tgname = :triggerName)";
        return Objects.equals(Boolean.TRUE, namedParameterJdbcTemplate.queryForObject(sql,
                Map.of("triggerName", getInternalLocalStateUpdateTriggerName(split[0], split[1])), Boolean.class));
    }

    @Override
    public Page<Map<String, Object>> getData(LocalDataCriteria localDataCriteria) {
        Map<String, Object> args = new HashMap<>();
        String sql = String.format("  FROM %s %n WHERE 1=1 ", escapeName(localDataCriteria.getSchemaTable()));
        String selectSubQuery = null;
        if(columnExists(LOADED_VERSION_REF, localDataCriteria.getSchemaTable())) {
            selectSubQuery = "(SELECT version FROM rdm_sync.loaded_version where id = version_id) as version_num ";
        } else {
            sql += " AND " + addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN) +" = :state";
            args.put("state", localDataCriteria.getState().name());
        }

        if (localDataCriteria.getRecordId() != null) {
            String sysPkColumn = localDataCriteria.getSysPkColumn();
            sql += "\n AND " + sysPkColumn + " = :" + sysPkColumn;
            args.put(sysPkColumn, localDataCriteria.getRecordId());
        }


        if (localDataCriteria.getDeleted() != null && localDataCriteria.getDeleted().getFieldName() != null) {
            String deletedFieldName = addDoubleQuotes(localDataCriteria.getDeleted().getFieldName());
            if (Boolean.TRUE.equals(localDataCriteria.getDeleted().isDeleted())) {
                sql += "\n AND " + deletedFieldName + " is not null";
            } else {
                sql += "\n AND " + deletedFieldName + " is null";
            }
        }

        Page<Map<String, Object>> data = getData0(sql, args, localDataCriteria, selectSubQuery);
        data.getContent().forEach(row -> {
            row.remove(RDM_SYNC_INTERNAL_STATE_COLUMN);
            row.remove(LOADED_VERSION_REF);
        });
        return data;
    }

    @Override
    public Page<Map<String, Object>> getSimpleVersionedData(VersionedLocalDataCriteria criteria) {
        Map<String, Object> args = new HashMap<>();
        String sql = String.format("%n  FROM %s %n WHERE 1=1 %n",
                escapeName(criteria.getSchemaTable()));

        if (criteria.getVersion() != null) {
            if (criteria.getRefBookCode() == null)
                throw new BadRequestException("refBookCode required if version not null");
            sql = sql + " AND " + LOADED_VERSION_REF + ("=(SELECT id from rdm_sync.loaded_version WHERE code = :code AND version = :version)");
            args.put("code", criteria.getRefBookCode());
            args.put("version", criteria.getVersion());
        }

        Page<Map<String, Object>> data = getData0(sql, args, criteria, null);
        data.getContent().forEach(row -> row.remove(LOADED_VERSION_REF));

        return data;
    }

    @Override
    public Page<Map<String, Object>> getVersionedData(VersionedLocalDataCriteria localDataCriteria) {

        Map<String, Object> args = new HashMap<>();
        String sql = String.format("%n  FROM %s %n WHERE 1=1 %n",
                escapeName(localDataCriteria.getSchemaTable()));

        if (localDataCriteria.getVersion() != null) {
            sql = sql + " AND " + VERSIONS_SYS_COL + " LIKE :" + VERSIONS_SYS_COL;
            args.put(VERSIONS_SYS_COL, "%{" + localDataCriteria.getVersion() + "}%");
        }

        Page<Map<String, Object>> data = getData0(sql, args, localDataCriteria, null);
        data.getContent().forEach(row -> {
            row.remove(VERSIONS_SYS_COL);
            row.remove(HASH_SYS_COL);
        });

        return data;
    }

    @Override
    public void upsertVersionedRows(String schemaTable, List<Map<String, Object>> rows, String version) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        StringJoiner columns = new StringJoiner(",");
        StringJoiner values = new StringJoiner(",");
        Map<String, Object>[] batchValues = new Map[rows.size()];
        concatColumnsAndValues(columns, values, batchValues, convertToVersionedRows(rows, version));

        final String sql = String.format("INSERT INTO %s (%s) VALUES(%s) ON CONFLICT ON CONSTRAINT  %s DO UPDATE SET _versions = %s._versions||'{%s}'",
                schemaTable, columns, values, "unique_hash", schemaTable, version);

        namedParameterJdbcTemplate.batchUpdate(sql, batchValues);
    }

    @Override
    public void upsertVersionedRows(String schemaTable, List<Map<String, Object>> rows, Integer loadedVersionId, String primaryKey) {
        if (CollectionUtils.isEmpty(rows)){
            return;
        }
        StringJoiner columns = new StringJoiner(",");
        StringJoiner values = new StringJoiner(",");
        Map<String, Object>[] batchValues = new Map[rows.size()];
        concatColumnsAndValues(columns, values, batchValues, rows);
        columns.add(escapeName(LOADED_VERSION_REF));
        values.add(String.valueOf(loadedVersionId));
        namedParameterJdbcTemplate.batchUpdate(String.format("INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s, %s) DO UPDATE SET (%s) = (%s);",
                escapeName(schemaTable), columns, values, escapeName(primaryKey), escapeName(LOADED_VERSION_REF), columns, values), batchValues);
    }

    private void concatColumnsAndValues(StringJoiner columns, StringJoiner values, Map<String, Object>[] batchValues, List<Map<String, Object>> rows) {

        int maxColumns = 0;
        Map<String, Object> longestRow = rows.get(0);
        for (Map<String, Object> row : rows) {
            if (row.keySet().size() > maxColumns) {
                maxColumns = row.keySet().size();
                longestRow = row;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> batchValue = new HashMap<>();
            for (String key : longestRow.keySet()) {
                // ставим null если нет ключей
                batchValue.put(key, row.get(key));

            }
            batchValues[i] = batchValue;
        }

        for (String key : longestRow.keySet()) {
            columns.add(escapeName(key));
            values.add(":" + key);
        }

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

        if (logger.isDebugEnabled()) {
            logger.debug("getData0 sql:\n{}\n binding args:\n{}\n.", sql, args);
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

        RestCriteria restCriteria = new AbstractCriteria();
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
    public <T> boolean setLocalRecordsState(String schemaTable, String pk, List<? extends T> primaryValues, RdmSyncLocalRowState expectedState, RdmSyncLocalRowState toState) {

        if (primaryValues.isEmpty())
            return false;

        String query = String.format("SELECT COUNT(*) FROM %s WHERE %s IN (:primaryValues)", schemaTable, addDoubleQuotes(pk));
        Integer count = namedParameterJdbcTemplate.queryForObject(query, Map.of("primaryValues", primaryValues), Integer.class);
        if (count == null || count == 0)
            return false;

        query = String.format("UPDATE %1$s SET %2$s = :toState WHERE %3$s IN (:primaryValues) AND %2$s = :expectedState", schemaTable, addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN), addDoubleQuotes(pk));
        int numUpdatedRecords = namedParameterJdbcTemplate.update(query, Map.of("toState", toState.name(), "primaryValues", primaryValues, "expectedState", expectedState.name()));
        return numUpdatedRecords == count;
    }

    @Override
    public RdmSyncLocalRowState getLocalRowState(String schemaTable, String pk, Object pv) {

        String query = String.format("SELECT %s FROM %s WHERE %s = :pv", addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN), schemaTable, addDoubleQuotes(pk));
        List<String> list = namedParameterJdbcTemplate.query(query, Map.of("pv", pv), (rs, rowNum) -> rs.getString(1));
        if (list.size() > 1)
            throw new RdmException("Cannot identify record by " + pk);

        return list.stream().findAny().map(RdmSyncLocalRowState::valueOf).orElse(null);
    }

    @Override
    public void createSchemaIfNotExists(String schema) {

        getJdbcTemplate().execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schema));
    }

    @Override
    public void createNotVersionedTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String isDeletedFieldName, String sysPkColumn, String primaryField) {
        if(isTableExists(schema, table)) {
            return;
        }
        createTable(schema, table, fieldMappings,
                Map.of(isDeletedFieldName, "timestamp without time zone",
                        sysPkColumn, RECORD_SYS_COL_INFO)
        );
        String addUniqueConstraint = "ALTER TABLE {schema}.{table} ADD CONSTRAINT {constraint} UNIQUE({column})";

        getJdbcTemplate().execute(StringSubstitutor.replace(
                addUniqueConstraint,
                Map.of("schema", escapeName(schema), "table", escapeName(table), "constraint", escapeName(table+"_uq"), "column", escapeName(primaryField)), "{", "}"));
    }

    @Override
    public void createTableWithNaturalPrimaryKeyIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String isDeletedFieldName, String sysPkColumn){
        if(isTableExists(schema, table)) {
            return;
        }
        createTable(schema, table, fieldMappings,  Map.of(isDeletedFieldName, "timestamp without time zone"));

        Boolean pkIsExists = getJdbcTemplate().queryForObject("SELECT EXISTS (SELECT * FROM pg_constraint " +
                "                   WHERE conrelid = ?::regclass and contype = 'p')", Boolean.class, schema+"."+table);
        if (!Boolean.TRUE.equals(pkIsExists)) {
            String sql = "ALTER TABLE " + escapeName(schema) + "." + escapeName(table) + " ADD CONSTRAINT "
                    + escapeName(table + "_pk") + " PRIMARY KEY (" + escapeName(sysPkColumn) + ");";

            getJdbcTemplate().execute(sql);
        }
    }

    @Override
    public void createVersionedTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String sysPkColumn) {

        if(isTableExists(schema, table)){
            return;
        }
        createTable(schema, table, fieldMappings,
                Map.of(VERSIONS_SYS_COL, "text NOT NULL",
                        HASH_SYS_COL, "text NOT NULL",
                        sysPkColumn, RECORD_SYS_COL_INFO)
        );

        getJdbcTemplate().execute(String.format("ALTER TABLE %s.%s ADD CONSTRAINT unique_hash UNIQUE (\"_hash\")", escapeName(schema), escapeName(table)));
    }

    @Override
    public void createSimpleVersionedTable(String schema, String table, List<FieldMapping> fieldMappings, String primaryField) {
        Boolean tableExists = isTableExists(schema, table);
        if (Boolean.FALSE.equals(tableExists)) {
            createTable(schema, table, fieldMappings,
                    Map.of(LOADED_VERSION_REF, "integer NOT NULL",
                            RECORD_SYS_COL, RECORD_SYS_COL_INFO)
            );

            String escapedSchemaTable = escapeName(schema) + "." + escapeName(table);
            getJdbcTemplate().execute(
                    String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES rdm_sync.loaded_version(id)",
                            escapedSchemaTable, escapeName(table + "_" + LOADED_VERSION_REF + "_fk"), LOADED_VERSION_REF
                    )
            );
            getJdbcTemplate().execute(
                    String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s, %s);",
                    escapedSchemaTable, escapeName(table + "_uq"), escapeName(primaryField), LOADED_VERSION_REF));
        }
    }

    private boolean isTableExists(String schema, String table) {
        return getJdbcTemplate().queryForObject(
                "SELECT EXISTS (SELECT FROM pg_tables  WHERE  schemaname = ? AND tablename  = ?)"
                , Boolean.class, schema, table);
    }

    @Override
    public SyncRefBook getSyncRefBook(String code) {
        List<SyncRefBook> result = namedParameterJdbcTemplate.query("select * from rdm_sync.refbook where code = :code", Map.of("code", code),
                (rs, rowNum) ->
                        new SyncRefBook(
                                rs.getInt("id"),
                                rs.getString("code"),
                                SyncTypeEnum.valueOf(rs.getString("sync_type")),
                                rs.getString("name"),
                                getRangeData(rs.getInt("id"))
                        )
        );
        if (result.isEmpty())
            return null;

        return result.get(0);
    }

    @Override
    public List<SyncRefBook> getActualSyncRefBooks() {
        return namedParameterJdbcTemplate.query("select * from rdm_sync.refbook where exists(select 1 from rdm_sync.version where ref_id = rdm_sync.refbook .id) ",
                (rs, rowNum) ->
                        new SyncRefBook(
                                rs.getInt("id"),
                                rs.getString("code"),
                                SyncTypeEnum.valueOf(rs.getString("sync_type")),
                                rs.getString("name"),
                                getRangeData(rs.getInt("id"))
                        )
        );
    }

    @Override
    public void createVersionTempDataTbl(String tempTableName, String refTableName, String sysPkColumn, String refPk) {
        String escapedTempTable = escapeName(tempTableName);
        getJdbcTemplate().execute("CREATE UNLOGGED TABLE " + escapedTempTable + " AS TABLE " + escapeName(refTableName) + " WITH NO DATA;"
                + " ALTER TABLE " + escapedTempTable + " DROP COLUMN IF EXISTS version_id;");
        if (sysPkColumn != null  && !sysPkColumn.equals(refPk)) {
            getJdbcTemplate().execute(" ALTER TABLE " + escapedTempTable + " DROP COLUMN IF EXISTS  " + escapeName(sysPkColumn));
        }
        getJdbcTemplate().execute("CREATE UNIQUE INDEX ON " + escapedTempTable + "(" + escapeName(refPk) + ");");
    }

    @Override
    public void createDiffTempDataTbl(String tempTableName, String refTableName) {
        String escapedTempTable = escapeName(tempTableName);
        String escapedRefTable = escapeName(refTableName);
        String queryTemplate = "CREATE TABLE {tempTbl} AS TABLE {refTbl} WITH NO DATA; ALTER TABLE {tempTbl} ADD COLUMN diff_type VARCHAR;";
        getJdbcTemplate().execute(StringSubstitutor.replace(queryTemplate, Map.of("tempTbl", escapedTempTable, "refTbl", escapedRefTable),"{", "}"));

    }

    @Override
    public void insertVersionAsTempData(String tableName, List<Map<String, Object>> data) {
        if(data.isEmpty()) {
            return;
        }
        List<String> columns = data.stream().flatMap(map -> map.keySet().stream()).distinct().collect(toList());
        String paramsExpression = columns.stream().map(column -> "?").collect(Collectors.joining(","));
        getJdbcTemplate().batchUpdate(
                "INSERT INTO " + escapeName(tableName) + "(" + columns.stream().map(this::escapeName).collect(joining(",")) + ")  VALUES(" + paramsExpression + ") ",
                data.stream().map(map -> {
                    Object[] params = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        if(map.get(columns.get(i)) instanceof List) {
                            params[i] = createSqlArray((List<Integer>) map.get(columns.get(i)));
                        } else {
                            params[i] = map.get(columns.get(i));
                        }
                    }
                    return params;
                }).collect(Collectors.toList()));
    }

    @Override
    public void insertDiffAsTempData(String tableName, List<Map<String, Object>> newData, List<Map<String, Object>> updatedData, List<Map<String, Object>> deletedData) {
        String escapedTable = escapeName(tableName);
        List<String> columns = Stream.of(newData, updatedData, deletedData)
                .flatMap(Collection::stream)
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .collect(Collectors.toUnmodifiableList());
        StringJoiner valuesJoiner = new StringJoiner(",");
        StringJoiner columnsJoiner = new StringJoiner(",");
        columns.forEach(column -> {
            valuesJoiner.add("?");
            columnsJoiner.add(escapeName(column));
        });
        String queryTemplate = "INSERT INTO {tempTbl}({columns}, diff_type) VALUES({values}, ?) ";

        String query = StringSubstitutor.replace(queryTemplate, Map.of("tempTbl", escapedTable, "columns", columnsJoiner.toString(), "values", valuesJoiner.toString()), "{", "}");

        BiFunction<Map<String, Object>, String, Object[]> toValuesArr = (map, diffType) -> {
            Object[] params = new Object[columns.size()+1];

            for (int i = 0; i < columns.size(); i++) {
                params[i] = map.get(columns.get(i));
            }
            params[columns.size()] = diffType;
            return params;
        };
        getJdbcTemplate().batchUpdate(query, newData.stream().map(map -> toValuesArr.apply(map, "I")).collect(Collectors.toList()));
        getJdbcTemplate().batchUpdate(query, updatedData.stream().map(map -> toValuesArr.apply(map, "U")).collect(Collectors.toList()));
        getJdbcTemplate().batchUpdate(query, deletedData.stream().map(map -> toValuesArr.apply(map, "D")).collect(Collectors.toList()));
    }

    @Override
    public void migrateNotVersionedTempData(String tempTable, String refTable, String pkField, String deletedField, List<String> fields, LocalDateTime deletedTime) {
        String revertDeletedRowsQueryTemplate = "UPDATE {refTbl} SET({columns}, {deletedField}) = (SELECT {columns}, null::timestamp without time zone  FROM {tempTbl} WHERE {pk} = {refTbl}.{pk}) " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE  {pk} = {refTbl}.{pk}) AND {deletedField} IS NOT NULL;";
        String addNewRowsQueryTemplate  = "INSERT INTO {refTbl}({columns}, rdm_sync_internal_local_row_state) " +
                "SELECT {columns}, 'SYNCED' FROM {tempTbl} WHERE NOT EXISTS (SELECT 1 FROM {refTbl} WHERE {pk} = {tempTbl}.{pk} );";
        String updateEditRowsQueryTemplate = "UPDATE {refTbl} SET({columns}) = (SELECT {columns} FROM {tempTbl} WHERE {pk} = {refTbl}.{pk}) " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE  {pk} = {refTbl}.{pk}) AND {deletedField} IS NULL;";
        String markDeletedQueryTemplate = "UPDATE {refTbl} SET {deletedField} = :deletedTime  WHERE NOT EXISTS(SELECT 1 FROM {tempTbl} WHERE {pk} = {refTbl}.{pk})  AND {deletedField} IS NULL;";
        String columns = fields.stream().map(this::escapeName).collect(Collectors.joining(","));

        String query = StringSubstitutor.replace(revertDeletedRowsQueryTemplate + addNewRowsQueryTemplate + updateEditRowsQueryTemplate + markDeletedQueryTemplate,
                Map.of("tempTbl", escapeName(tempTable), "columns", columns, "refTbl", escapeName(refTable), "pk", escapeName(pkField), "deletedField", escapeName(deletedField)), "{", "}");
        namedParameterJdbcTemplate.update(query, Map.of("deletedTime", deletedTime));
    }

    @Override
    public void migrateDiffTempData(String tempTable, String refTable, String pkField, String deletedField, List<String> fields, LocalDateTime deletedTime) {
        String revertDeletedRowsQueryTemplate = "UPDATE {refTbl} SET({columns}, {deletedField}) = (SELECT {columns}, null::timestamp without time zone  FROM {tempTbl} WHERE {pk} = {refTbl}.{pk}) " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE  {pk} = {refTbl}.{pk} AND diff_type = 'I' ) AND {deletedField} IS NOT NULL;";
        String insertQueryTemplate  = "INSERT INTO {refTbl}({columns}, rdm_sync_internal_local_row_state) " +
                "(SELECT {columns}, 'SYNCED' FROM {tempTbl} WHERE diff_type = 'I' AND NOT EXISTS(SELECT 1 FROM {refTbl} WHERE  {pk} = {tempTbl}.{pk} ));";
        String updateQueryTemplate = "UPDATE {refTbl} SET({columns}) = (SELECT {columns} FROM {tempTbl} WHERE {pk} = {refTbl}.{pk}) " +
                "WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE diff_type = 'U' AND {pk} = {refTbl}.{pk}) AND {deletedField} IS NULL;";
        String deleteQueryTemplate = "UPDATE {refTbl} SET {deletedField} = :deletedTime  WHERE EXISTS(SELECT 1 FROM {tempTbl} WHERE diff_type = 'D' AND {pk} = {refTbl}.{pk})  AND {deletedField} IS NULL;";
        String columns = fields.stream().map(this::escapeName).collect(Collectors.joining(","));

        String query = StringSubstitutor.replace(updateQueryTemplate + revertDeletedRowsQueryTemplate + insertQueryTemplate +  deleteQueryTemplate,
                Map.of("tempTbl", escapeName(tempTable), "columns", columns, "refTbl", escapeName(refTable), "pk", escapeName(pkField), "deletedField", escapeName(deletedField)), "{", "}");
        namedParameterJdbcTemplate.update(query, Map.of("deletedTime", deletedTime));

    }

    @Override
    public void migrateVersionedTempData(String tempTable, String refTable, String pkField, Integer versionId, List<String> fields) {
        String columnsExpression =  fields.stream().map(this::escapeName).collect(Collectors.joining(","));
        namedParameterJdbcTemplate.update("INSERT INTO " + escapeName(refTable) + "(" + columnsExpression + ", version_id) " +
                "(SELECT "+ columnsExpression + ", :versionId FROM " + escapeName(tempTable) + ")", Map.of("versionId", versionId));
    }

    @Override
    public void reMigrateVersionedTempData(String tempTable, String refTable, String pkField, Integer versionId, List<String> fields) {
        String escapedRefTbl = escapeName(refTable);
        String escapedPk = escapeName(pkField);
        String columnsExpression = fields.stream().map(this::escapeName).collect(Collectors.joining(","));
        String escapedTempTbl = escapeName(tempTable);
        Map<String, String> queryPlaceholders = Map.of("refTbl", escapedRefTbl, "columns", columnsExpression, "tempTbl", escapedTempTbl, "pk", escapedPk);

        //удаляем записи которых нет в версии
        String deleteQueryTemplate = "DELETE FROM {refTbl}" +
                " WHERE NOT EXISTS(SELECT 1 FROM {tempTbl}  WHERE {pk} = {refTbl}.{pk}) " +
                "AND version_id = :versionId";
        String deleteQuery = StringSubstitutor.replace(deleteQueryTemplate, queryPlaceholders,"{", "}");
        namedParameterJdbcTemplate.update(deleteQuery, Map.of("versionId", versionId));

        //Добавляем записи которые появились в версии
        String insertQueryTemplate = "INSERT INTO {refTbl}({columns}, version_id) " +
                "(SELECT {columns}, :versionId FROM {tempTbl} WHERE NOT exists(SELECT 1 FROM {refTbl} WHERE {pk} =  {tempTbl}.{pk} AND version_id = :versionId) )";
        String insertQuery = StringSubstitutor.replace( insertQueryTemplate, queryPlaceholders, "{", "}");
        namedParameterJdbcTemplate.update(insertQuery, Map.of("versionId", versionId));

        //редактируем записи которые изменились
        String updateQueryTemplate = "UPDATE {refTbl} SET ({columns}) = (SELECT {columns} FROM {tempTbl} WHERE {pk} = {refTbl}.{pk}) WHERE exists(SELECT 1 FROM {tempTbl} WHERE {pk} =  {refTbl}.{pk}) AND version_id = :versionId ";
        String updateQuery = StringSubstitutor.replace(updateQueryTemplate, queryPlaceholders,"{", "}");
        namedParameterJdbcTemplate.update(updateQuery, Map.of("versionId", versionId));
    }

    @Override
    public void dropTable(String tableName) {
        getJdbcTemplate().execute("DROP TABLE IF EXISTS " + escapeName(tableName));
    }

    @Override
    public List<VersionMapping> getVersionMappingsByRefBookCode(String refBookCode) {

        final String sql = "SELECT m.id, code, name, version, \n" +
                "       sys_table, sys_pk_field, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type, match_case, refreshable_range \n" +
                "  FROM rdm_sync.version v \n" +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id \n" +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id \n" +
                "WHERE code = :code;";

        return namedParameterJdbcTemplate.query(sql,Map.of("code",refBookCode),
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getString(8),
                        rs.getString(9),
                        toLocalDateTime(rs, 10, LocalDateTime.MIN),
                        rs.getInt(11),
                        rs.getInt(12),
                        SyncTypeEnum.valueOf(rs.getString(13)),
                        new Range(rs.getString(4)),
                        rs.getBoolean(14),
                        rs.getBoolean(15)
                )
        );
    }

    @Override
    public VersionMapping getVersionMappingByRefBookCodeAndRange(String code, String range) {
        final String sql = "SELECT m.id, code, name, version, " +
                "       sys_table, sys_pk_field, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, " +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type, match_case, refreshable_range " +
                "  FROM rdm_sync.version v " +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id " +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id " +
                "WHERE code = :code AND (:range IS NULL OR version = :range);";

        List<VersionMapping> results = namedParameterJdbcTemplate.query(sql, Map.of(
                "code", code,
                "range", range == null ? "" : range
        ), (rs, rowNum) -> new VersionMapping(
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(5),
                rs.getString(6),
                rs.getString(7),
                rs.getString(8),
                rs.getString(9),
                toLocalDateTime(rs, 10, LocalDateTime.MIN),
                rs.getInt(11),
                rs.getInt(12),
                SyncTypeEnum.valueOf(rs.getString(13)),
                new Range(rs.getString(4)),
                rs.getBoolean(14),
                rs.getBoolean(15)
        ));

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void deleteVersionMappings(Set<Integer> mappingIds) {
        // Удаляем запись из таблицы rdm_sync.field_mapping по mapping_id
        final String delFieldSql = "DELETE FROM rdm_sync.field_mapping WHERE mapping_id in (:mappingIds);";

        // Удаляем запись из таблицы rdm_sync.version по mapping_id
        final String delVersionSql = "DELETE FROM rdm_sync.version WHERE mapping_id in (:mappingIds);";

        // Удаляем запись из таблицы rdm_sync.mapping по id
        final String delMappingSql = "DELETE FROM rdm_sync.mapping WHERE id in (:mappingIds);";
        namedParameterJdbcTemplate.update(delFieldSql + delVersionSql + delMappingSql, Map.of("mappingIds", mappingIds));
    }

    @Override
    public List<String> getColumns(String schema, String table) {
        return namedParameterJdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = :schema AND table_name   = :table",
                Map.of("schema", schema, "table", table),
                String.class);
    }

    @Override
    public Boolean tableExists(String schema, String table) {
        return namedParameterJdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT * FROM information_schema.tables  WHERE table_schema = :schema AND table_name = :table",
                Map.of("schema", schema, "table", table),
                Boolean.class);
    }


    @Override
    public void refreshTable(String schema, String table, List<FieldMapping> newFieldMappings) {
        StringBuilder ddl = new StringBuilder(String.format("ALTER TABLE %s.%s ", escapeName(schema), escapeName(table)));

        ddl.append(newFieldMappings.stream()
                .map(mapping -> String.format(" ADD COLUMN %s %s", escapeName(mapping.getSysField()), mapping.getSysDataType()))
                .collect(Collectors.joining(", ")));

        getJdbcTemplate().execute(ddl.toString());
    }

    private void createTable(String schema, String table,
                             List<FieldMapping> fieldMappings,
                             Map<String, String> additionalColumns) {

        StringBuilder ddl = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s.%s (", escapeName(schema), escapeName(table)));

        ddl.append(fieldMappings.stream()
                .map(mapping -> String.format("%s %s", escapeName(mapping.getSysField()), mapping.getSysDataType()))
                .collect(Collectors.joining(", ")));
        for (Map.Entry<String, String> entry : additionalColumns.entrySet()) {
            ddl.append(String.format(", %s %s", escapeName(entry.getKey()), entry.getValue()));
        }
        ddl.append(")");

        getJdbcTemplate().execute(ddl.toString());
    }

    private JdbcTemplate getJdbcTemplate() {

        return namedParameterJdbcTemplate.getJdbcTemplate();
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, int columnIndex, LocalDateTime defaultValue) throws SQLException {

        final Timestamp value = rs.getTimestamp(columnIndex);
        return value != null ? value.toLocalDateTime() : defaultValue;
    }

    private String getInternalLocalStateUpdateTriggerName(String schema, String table) {

        return schema + "_" + table + "_intrnl_lcl_rw_stt_updt";
    }

    private String escapeName(String name) {
        if (name.contains(";")) {
            throw new IllegalArgumentException(name + "illegal value");
        }
        if (name.contains(".")) {
            String firstPart = escapeName(name.split("\\.")[0]);
            String secondPart = escapeName(name.split("\\.")[1]);
            return firstPart + "." + secondPart;
        }
        return "\"" + name + "\"";

    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null)
            return null;

        return timestamp.toLocalDateTime();
    }

    private boolean columnExists(String columnName, String schemaTable) {
        return namedParameterJdbcTemplate.queryForObject("select exists (select 1\n" +
                "FROM information_schema.columns\n" +
                "where table_schema||'.'||table_name=:schemaTable and column_name= :colName)",
                Map.of("schemaTable", schemaTable, "colName", columnName), Boolean.class);
    }

    private java.sql.Array createSqlArray(List<?> list) {
        if (list.isEmpty())
            return null;
        if (list.get(0) instanceof Integer) {
            Integer[] result = list.toArray(new Integer[0]);
            return namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    (Connection c) -> c.createArrayOf(JDBCType.INTEGER.getName(), result)
            );
        } else {
            String[] result = list.toArray(new String[0]);
            return namedParameterJdbcTemplate.getJdbcTemplate().execute(
                    (Connection c) -> c.createArrayOf(JDBCType.VARCHAR.getName(), result)
            );
        }

    }

    private Set<String> getRangeData(int refId) {
        List<String> rangeData = namedParameterJdbcTemplate.query("select version from rdm_sync.version where ref_id = :refId",
                Map.of("refId", refId),
                (rs, rowNum) -> rs.getString("version")
        );

        return new HashSet<>(rangeData);
    }
}


