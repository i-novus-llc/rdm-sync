package ru.i_novus.ms.rdm.sync.dao;

import net.n2oapp.platform.jaxrs.RestCriteria;
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
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MultivaluedMap;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
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
    private static final String VERSIONS_SYS_COL = "_versions";
    private static final String HASH_SYS_COL = "_hash";

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private RdmMappingService rdmMappingService;

    @Override
    public List<VersionMapping> getVersionMappings() {

        final String sql = "SELECT m.id, code, version, \n" +
                "       sys_table, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type \n" +
                "  FROM rdm_sync.version v \n" +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id \n" +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id \n";

        return namedParameterJdbcTemplate.query(sql,
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        toLocalDateTime(rs, 8, LocalDateTime.MIN),
                        rs.getInt(9),
                        rs.getInt(10),
                        SyncTypeEnum.valueOf(rs.getString(11))
                )
        );
    }

    @Override
    public LoadedVersion getLoadedVersion(String code) {
        List<LoadedVersion> result = namedParameterJdbcTemplate.query("select * from rdm_sync.loaded_version where code = :code", Map.of("code", code),
                (ResultSet rs, int rowNum) -> new LoadedVersion(rs.getInt("id"), rs.getString("code"), rs.getString("version"), rs.getTimestamp("publication_dt").toLocalDateTime(), rs.getTimestamp("update_dt").toLocalDateTime())
        );

        if(CollectionUtils.isEmpty(result)) {
            return null;
        }

        return result.get(0);

    }

    @Override
    public VersionMapping getVersionMapping(String refbookCode, String version) {
        final String sql = "SELECT m.id, code, version, \n" +
                "       sys_table, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type \n" +
                "  FROM rdm_sync.version v \n" +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id \n" +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id \n" +
                " WHERE code = :code and version = :version \n";

        List<VersionMapping> list = namedParameterJdbcTemplate.query(sql,
                Map.of("code", refbookCode, "version", version),
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        toLocalDateTime(rs, 8, LocalDateTime.MIN),
                        rs.getInt(9),
                        rs.getInt(10),
                        SyncTypeEnum.valueOf(rs.getString(11))
                )
        );
        return !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public int getLastVersion(String refbookCode) {

        final String sql = "SELECT lv.version FROM rdm_sync.loaded_version lv WHERE lv.code = :code";

        List<Integer> list = namedParameterJdbcTemplate.query(sql,
                Map.of("code", refbookCode),
                (rs, rowNum) -> rs.getInt(1)
        );

        return !list.isEmpty() ? list.get(0) : 0;
    }

    @Override
    public List<FieldMapping> getFieldMappings(String refbookCode) {

        final String sql = "SELECT m.sys_field, m.sys_data_type, m.rdm_field \n" +
                "  FROM rdm_sync.field_mapping m \n" +
                " WHERE m.mapping_id = ( \n" +
                "       SELECT v.mapping_id \n" +
                "         FROM rdm_sync.version v \n" +
                "        WHERE v.ref_id = ( \n" +
                "              SELECT r.id FROM rdm_sync.refbook r WHERE r.code = :code \n" +
                "              )" +
                "          AND v.version = :version \n" +
                "       ) \n";

        return namedParameterJdbcTemplate.query(sql,
            Map.of("code", refbookCode, "version", "CURRENT"),
            (rs, rowNum) -> new FieldMapping(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3)
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
    public List<Object> getDataIds(String schemaTable, FieldMapping primaryFieldMapping) {

        final String sql = String.format("SELECT %s FROM %s",
                addDoubleQuotes(primaryFieldMapping.getSysField()), schemaTable);

        DataTypeEnum dataType = DataTypeEnum.getByDataType(primaryFieldMapping.getSysDataType());
        return namedParameterJdbcTemplate.query(sql,
            (rs, rowNum) -> rdmMappingService.map(AttributeTypeEnum.STRING, dataType, rs.getObject(1))
        );
    }

    @Override
    public boolean isIdExists(String schemaTable, String primaryField, Object primaryValue) {

        final String sql = String.format("SELECT count(*) > 0 FROM %s WHERE %s = :primary",
                schemaTable, addDoubleQuotes(primaryField));

        final Boolean result = namedParameterJdbcTemplate.queryForObject(sql,
                Map.of("primary", primaryValue),
                Boolean.class
        );
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void insertLoadedVersion(String code, String version, LocalDateTime publishDate) {
        final String sql = "INSERT INTO rdm_sync.loaded_version(code, version, publication_dt, update_dt) \n" +
                "   VALUES(:code, :version, :publishDate, :updateDate) ";

        namedParameterJdbcTemplate.update(sql,
                Map.of("version", version,
                        "publishDate", publishDate,
                        "updateDate", LocalDateTime.now(Clock.systemUTC()),
                        "code", code)
        );
    }

    @Override
    public void updateLoadedVersion(Integer id, String version, LocalDateTime publishDate) {

        final String sql = "UPDATE rdm_sync.loaded_version \n" +
                "   SET version = :version, \n" +
                "       publication_dt = :publication_dt, \n" +
                "       update_dt = :update_dt \n" +
                " WHERE id = :id";

        namedParameterJdbcTemplate.update(sql,
                Map.of("version", version,
                        "publication_dt", publishDate,
                        "update_dt", LocalDateTime.now(Clock.systemUTC()),
                        "id", id)
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
        }).collect(Collectors.toList());
        insertRows(schemaTable, newRows);
    }

    @Override
    public void insertVersionedRows(String schemaTable, List<Map<String, Object>> rows, String version) {
        insertRows(schemaTable, convertToVersionedRows(rows, version));
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
                            Object primaryValue, boolean deleted, boolean markSynced) {

        Map<String, Object> args = markSynced
                ? Map.of(primaryField, primaryValue,
                isDeletedField, deleted,
                RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name())
                : Map.of(primaryField, primaryValue,
                isDeletedField, deleted);

        executeUpdate(schemaTable, Collections.singletonList(args), primaryField);
    }

    @Override
    public void markDeleted(String schemaTable, String isDeletedField, boolean deleted, boolean markSynced) {

        Map<String, Object> args = markSynced
                ? Map.of(isDeletedField, deleted,
                RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name())
                : Map.of(isDeletedField, deleted);

        executeUpdate(schemaTable, Collections.singletonList(args), null);
    }

    private List<Map<String, Object>> convertToVersionedRows(List<Map<String, Object>> rows, String version) {
        return rows.stream().map(row -> {
            Map<String, Object> newMap = new HashMap<>(row);
            StringBuilder stringBuilder = new StringBuilder();
            row.entrySet().stream()
                    .filter( entry -> entry.getValue() != null)
                    .forEach(entry -> stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
            newMap.put(HASH_SYS_COL, DigestUtils.md5DigestAsHex(stringBuilder.toString().getBytes(StandardCharsets.UTF_8)));
            newMap.put(VERSIONS_SYS_COL, "{" + version + "}");
            return newMap;
        }).collect(Collectors.toList());
    }

    private void insertRows(String schemaTable, List<Map<String, Object>> rows) {
        if(CollectionUtils.isEmpty(rows)){
            return;
        }

        StringJoiner columns = new StringJoiner(",");
        StringJoiner values = new StringJoiner(",");
        Map<String, Object>[] batchValues = new Map[rows.size()];
        concatColumnsAndValues(columns, values, batchValues, rows);

        final String sql = String.format("INSERT INTO %s (%s) VALUES(%s)",
                escapeName(schemaTable), columns.toString(), values.toString());

        namedParameterJdbcTemplate.batchUpdate(sql, batchValues);
    }


    private void executeUpdate(String table, List<Map<String, Object>> rows, String primaryField) {

        if(CollectionUtils.isEmpty(rows)){
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

        String sql = String.format(sqlFormat, table, fields, addDoubleQuotes(primaryField), primaryField);

        Map<String, Object>[] batchValues = new Map[rows.size()];
        namedParameterJdbcTemplate.batchUpdate(sql, rows.toArray(batchValues));
    }

    @Override
    public void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack) {

        final String sql = "INSERT INTO rdm_sync.log \n" +
                "      (code, current_version, new_version, status, date, message, stack) \n" +
                "VALUES(?,?,?,?,?,?,?)";

        getJdbcTemplate().update(sql,
            refbookCode, oldVersion, newVersion, status, new Date(), message, stack
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
                "    unique_sys_field)\n" +
                "values (\n" +
                "    :deleted_field,\n" +
                "    :mapping_version,\n" +
                "    :sys_table,\n" +
                "    :unique_sys_field) RETURNING id";

        Integer mappingId = namedParameterJdbcTemplate.queryForObject(insMappingSql,
                Map.of("deleted_field", versionMapping.getDeletedField(),
                        "mapping_version", versionMapping.getMappingVersion(),
                        "sys_table", versionMapping.getTable(),
                        "unique_sys_field", versionMapping.getPrimaryField()),
                Integer.class
        );

        final String insRefSql = "insert into rdm_sync.refbook(code, source_id, sync_type) values(:code, (SELECT id FROM rdm_sync.source WHERE code=:source_code), :type)  RETURNING id";
        Integer refBookId = namedParameterJdbcTemplate.queryForObject(insRefSql,
                Map.of("code", versionMapping.getCode(), "source_code",versionMapping.getSource(), "type", versionMapping.getType().name()),
                Integer.class);

        namedParameterJdbcTemplate.update("insert into rdm_sync.version(ref_id, mapping_id, version) values(:refId, :mappingId, :version)",
                Map.of("refId", refBookId, "mappingId", mappingId, "version", versionMapping.getVersion() != null ? versionMapping.getVersion() : "CURRENT"));

        return mappingId;
    }

    @Override
    public void updateCurrentMapping(VersionMapping versionMapping) {
        final String sql = "update rdm_sync.mapping set deleted_field = :deleted_field, mapping_version = :mapping_version, sys_table = :sys_table, unique_sys_field = :unique_sys_field" +
                " where id = (select mapping_id from rdm_sync.version where version = 'CURRENT' and ref_id = (select id from rdm_sync.refbook where code = :code))";
        namedParameterJdbcTemplate.update(sql,
                Map.of("deleted_field", versionMapping.getDeletedField(),
                        "mapping_version", versionMapping.getMappingVersion(),
                        "sys_table", versionMapping.getTable(), "unique_sys_field",
                        versionMapping.getPrimaryField(), "code", versionMapping.getCode()));
    }

    @Override
    public void insertFieldMapping(Integer mappingId, List<FieldMapping> fieldMappings) {

        final String sqlDelete = "DELETE FROM rdm_sync.field_mapping WHERE mapping_id = ?";

        getJdbcTemplate().update(sqlDelete, mappingId);

        final String sqlInsert = "INSERT INTO rdm_sync.field_mapping \n" +
                "      (mapping_id, sys_field, sys_data_type, rdm_field) \n" +
                "VALUES(?, ?, ?, ?)";

        getJdbcTemplate().batchUpdate(sqlInsert,
                new BatchPreparedStatementSetter() {

                    public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {

                        ps.setInt(1, mappingId);
                        ps.setString(2, fieldMappings.get(i).getSysField());
                        ps.setString(3, fieldMappings.get(i).getSysDataType());
                        ps.setString(4, fieldMappings.get(i).getRdmField());
                    }

                    public int getBatchSize() {
                        return fieldMappings.size();
                    }

                });
    }

    @Override
    public boolean lockRefBookForUpdate(String code, boolean blocking) {

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

        String triggerName = getInternalLocalStateUpdateTriggerName(schema, table);
        String schemaTable = schema + "." + table;

        final String sqlExists = "SELECT EXISTS(SELECT 1 FROM pg_trigger WHERE NOT tgisinternal AND tgname = :tgname)";
        Boolean exists = namedParameterJdbcTemplate.queryForObject(sqlExists, Map.of("tgname", triggerName), Boolean.class);

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

        String schemaTable = schema + "." + table;
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
        String query = String.format("ALTER TABLE %s DISABLE TRIGGER %s", table, getInternalLocalStateUpdateTriggerName(split[0], split[1]));

        getJdbcTemplate().execute(query);
    }

    @Override
    public void enableInternalLocalRowStateUpdateTrigger(String table) {

        String[] split = table.split("\\.");
        String query = String.format("ALTER TABLE %s ENABLE TRIGGER %s", table, getInternalLocalStateUpdateTriggerName(split[0], split[1]));

        getJdbcTemplate().execute(query);
    }

    @Override
    public Page<Map<String, Object>> getData(LocalDataCriteria localDataCriteria) {

        Map<String, Object> args = new HashMap<>();
        String sql = String.format("  FROM %s %n WHERE %s = :state %n",
                localDataCriteria.getSchemaTable(), addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN));
        args.put("state", localDataCriteria.getState().name());
        if(localDataCriteria.getDeleted() != null) {
            if(Boolean.TRUE.equals(localDataCriteria.getDeleted().isDeleted())) {
                sql += " AND " + addDoubleQuotes(localDataCriteria.getDeleted().getFieldName()) + " = true";
            } else {
                sql += " AND coalesce(" + addDoubleQuotes(localDataCriteria.getDeleted().getFieldName()) + ", false) = false";
            }
        }

        Page<Map<String, Object>> data = getData0(sql, args, localDataCriteria);
        data.getContent().forEach(row -> row.remove(RDM_SYNC_INTERNAL_STATE_COLUMN));
        return data;
    }

    @Override
    public Page<Map<String, Object>> getVersionedData(VersionedLocalDataCriteria localDataCriteria) {
        Map<String, Object> args = new HashMap<>();
        String sql = String.format("  FROM %s %n WHERE 1=1 %n",
                escapeName(localDataCriteria.getSchemaTable()));
        if(localDataCriteria.getVersion() != null) {
            sql = sql + " AND _versions like :versions";
            args.put("versions", "%{" + localDataCriteria.getVersion() + "}%");
        }
        Page<Map<String, Object>> data = getData0(sql, args, localDataCriteria);
        data.getContent().forEach(row -> {
            row.remove(VERSIONS_SYS_COL);
            row.remove(HASH_SYS_COL);
        });
        return data;
    }

    @Override
    public void upsertVersionedRows(String schemaTable, List<Map<String, Object>> rows, String version) {
        if(CollectionUtils.isEmpty(rows)){
            return;
        }
        StringJoiner columns = new StringJoiner(",");
        StringJoiner values = new StringJoiner(",");
        Map<String, Object>[] batchValues = new Map[rows.size()];
        concatColumnsAndValues(columns, values, batchValues, convertToVersionedRows(rows, version));

        final String sql = String.format("INSERT INTO %s (%s) VALUES(%s) ON CONFLICT ON CONSTRAINT  %s DO UPDATE SET _versions = %s._versions||'{%s}'",
                schemaTable, columns.toString(), values.toString(), "unique_hash", schemaTable, version);

        namedParameterJdbcTemplate.batchUpdate(sql, batchValues);
    }

    private void concatColumnsAndValues(StringJoiner columns, StringJoiner values, Map<String, Object>[] batchValues,  List<Map<String, Object>> rows) {

        int maxColumns = 0;
        Map<String, Object> longestRow = rows.get(0);
        for (Map<String, Object> row : rows) {
            if(row.keySet().size() > maxColumns) {
                maxColumns = row.keySet().size();
                longestRow = row;
            }
        }
        for(int i=0; i<rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> batchValue = new HashMap<>();
            for(String key : longestRow.keySet()) {
                // ставим null если нет ключей
                batchValue.put(key, row.get(key));

            }
            batchValues[i] = batchValue;
        }

        for (String key: longestRow.keySet()) {
            columns.add(escapeName(key));
            values.add(":" + key);
        }

    }

    private Page<Map<String, Object>> getData0(String sql, Map<String, Object> args, BaseDataCriteria dataCriteria) {

        MultivaluedMap<String, Object> filters = dataCriteria.getFilters();
        if (filters != null) {
            args.putAll(filters);

            sql += filters.keySet().stream()
                    .map(key -> "   AND " + addDoubleQuotes(key) + " IN (:" + key + ")")
                    .collect(joining("\n"));
        }

        Integer count = namedParameterJdbcTemplate.queryForObject("SELECT count(*) \n" + sql, args, Integer.class);
        if (count == null || count == 0)
            return Page.empty();

        String pk = dataCriteria.getPk();
        int limit = dataCriteria.getLimit();
        sql += String.format(" ORDER BY %s %n LIMIT %d OFFSET %d", addDoubleQuotes(pk), limit, dataCriteria.getOffset());

        List<Map<String, Object>> result = namedParameterJdbcTemplate.query("SELECT * \n" + sql,
                args, (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            Object val = rs.getObject(i);
                            String key = rs.getMetaData().getColumnName(i);
                            map.put(key, val);
                    }

                    return map;
                });

        RestCriteria restCriteria = new AbstractCriteria();
        restCriteria.setPageNumber(dataCriteria.getOffset() / limit);
        restCriteria.setPageSize(limit);
        restCriteria.setOrders(Sort.by(Sort.Order.asc(pk)).get().collect(Collectors.toList()));

        return new PageImpl<>(result, restCriteria, count);
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
    public void createTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String isDeletedFieldName) {

        createTable(schema, table, fieldMappings,
                Map.of(isDeletedFieldName, "BOOLEAN",
                        RECORD_SYS_COL, "BIGSERIAL")
        );
    }

    @Override
    public void createVersionedTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings) {

        createTable(schema, table, fieldMappings,
                Map.of(VERSIONS_SYS_COL, "text NOT NULL",
                        HASH_SYS_COL, "text NOT NULL",
                        RECORD_SYS_COL, "BIGSERIAL")
        );

        getJdbcTemplate().execute(String.format("ALTER TABLE %s.%s ADD CONSTRAINT unique_hash UNIQUE (\"_hash\")", escapeName(schema), escapeName(table)));
    }

    @Override
    public SyncRefBook getSyncRefBook(String code) {
        List<SyncRefBook> result = namedParameterJdbcTemplate.query("select * from rdm_sync.refbook where code = :code", Map.of("code", code),
                (rs, rowNum) ->
                        new SyncRefBook(
                                rs.getInt("id"),
                                rs.getString("code"),
                                SyncTypeEnum.valueOf(rs.getString("sync_type")),
                                rs.getString("name")
                        )
        );
        if(result.isEmpty())
            return null;

        return result.get(0);
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

    private String escapeName(String name){
        if(name.contains(";")) {
            throw new IllegalArgumentException(name + "illegal value");
        }
        if(name.contains(".")) {
            String firstPart = escapeName(name.split("\\.")[0]);
            String secondPart = escapeName(name.split("\\.")[1]);
            return firstPart + "." + secondPart;
        }
        return "\"" + name + "\"";

    }
}
