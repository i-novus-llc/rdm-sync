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
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.i_novus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MultivaluedMap;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addDoubleQuotes;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addSingleQuotes;
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

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private RdmMappingService rdmMappingService;

    @Override
    public List<VersionMapping> getVersionMappings() {

        final String sql = "SELECT id, code, version, publication_dt, \n" +
                "       sys_table, unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, update_dt \n" +
                "  FROM rdm_sync.version";

        return namedParameterJdbcTemplate.query(sql,
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        toLocalDateTime(rs, 4, null),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        toLocalDateTime(rs, 8, LocalDateTime.MIN),
                        toLocalDateTime(rs, 9, LocalDateTime.MIN)
                )
        );
    }

    @Override
    public VersionMapping getVersionMapping(String refbookCode) {

        final String sql = "SELECT id, code, version, publication_dt, \n" +
                "       sys_table, unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, update_dt \n" +
                "  FROM rdm_sync.version \n" +
                " WHERE code = :code";

        List<VersionMapping> list = namedParameterJdbcTemplate.query(sql,
                Map.of("code", refbookCode),
                (rs, rowNum) -> new VersionMapping(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        toLocalDateTime(rs, 4, null),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        toLocalDateTime(rs, 8, LocalDateTime.MIN),
                        toLocalDateTime(rs, 9, LocalDateTime.MIN)
                )
        );
        return !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public int getLastVersion(String refbookCode) {

        final String sql = "SELECT mapping_version FROM rdm_sync.version WHERE code = :code";

        List<Integer> list = namedParameterJdbcTemplate.query(sql,
                Map.of("code", refbookCode),
                (rs, rowNum) -> rs.getInt(1)
        );

        return !list.isEmpty() ? list.get(0) : 0;
    }

    @Override
    public List<FieldMapping> getFieldMappings(String refbookCode) {

        final String sql = "SELECT sys_field, sys_data_type, rdm_field \n" +
                "  FROM rdm_sync.field_mapping \n" +
                " WHERE code = :code";

        return namedParameterJdbcTemplate.query(sql,
            Map.of("code", refbookCode),
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
    public void updateVersionMapping(Integer id, String version, LocalDateTime publishDate) {

        final String sql = "UPDATE rdm_sync.version \n" +
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

        List<String> values = new ArrayList<>();
        List<Object> data = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() == null) {
                values.add("null");
            } else {
                values.add("?");
                data.add(entry.getValue());
            }
        }

        String keys = row.keySet().stream().map(StringUtils::addDoubleQuotes).collect(joining(","));
        if (markSynced) {
            keys += ", " + addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN);
            values.add(addSingleQuotes(SYNCED.name()));
        }

        final String sql = String.format("INSERT INTO %s (%s) VALUES(%s)",
                schemaTable, keys, String.join(",", values));
        getJdbcTemplate().update(sql, data.toArray());
    }

    @Override
    public void updateRow(String schemaTable, String primaryField, Map<String, Object> row, boolean markSynced) {

        if (markSynced) {
            row.put(RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name());
        }

        executeUpdate(schemaTable, row, primaryField);
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

        executeUpdate(schemaTable, args, primaryField);
    }

    @Override
    public void markDeleted(String schemaTable, String isDeletedField, boolean deleted, boolean markSynced) {

        Map<String, Object> args = markSynced
                ? Map.of(isDeletedField, deleted,
                RDM_SYNC_INTERNAL_STATE_COLUMN, SYNCED.name())
                : Map.of(isDeletedField, deleted);

        executeUpdate(schemaTable, args, null);
    }

    private void executeUpdate(String table, Map<String, Object> args, String primaryField) {

        String sqlFormat = "UPDATE %s SET %s";
        if (primaryField != null) {
            sqlFormat += " WHERE %s = :%s";
        }

        final String fields = args.keySet().stream()
                .filter(field -> !field.equals(primaryField))
                .map(field -> addDoubleQuotes(field) + " = :" + field)
                .collect(joining(", "));

        String sql = String.format(sqlFormat, table, fields, addDoubleQuotes(primaryField), primaryField);

        namedParameterJdbcTemplate.update(sql, args);
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
    public void upsertVersionMapping(XmlMappingRefBook versionMapping) {

        final String sql = "INSERT INTO rdm_sync.version \n" +
                "      (code, sys_table, unique_sys_field, deleted_field, mapping_version) \n" +
                "VALUES(?, ?, ?, ?, ? ) \n" +
                "ON CONFLICT (code) \n" +
                "DO UPDATE \n" +
                "   SET (sys_table, unique_sys_field, deleted_field, mapping_version) = \n" +
                "       (?, ?, ?, ?)";

        getJdbcTemplate().update(sql,
                // insert:
                versionMapping.getCode(),
                versionMapping.getSysTable(), versionMapping.getUniqueSysField(),
                versionMapping.getDeletedField(), versionMapping.getMappingVersion(),
                // update:
                versionMapping.getSysTable(), versionMapping.getUniqueSysField(),
                versionMapping.getDeletedField(), versionMapping.getMappingVersion()
        );
    }

    @Override
    public void insertFieldMapping(String code, List<XmlMappingField> fieldMappings) {

        final String sqlDelete = "DELETE FROM rdm_sync.field_mapping WHERE code = ?";

        getJdbcTemplate().update(sqlDelete, code);

        final String sqlInsert = "INSERT INTO rdm_sync.field_mapping \n" +
                "      (code, sys_field, sys_data_type, rdm_field) \n" +
                "VALUES(?, ?, ?, ?)";

        getJdbcTemplate().batchUpdate(sqlInsert,
                new BatchPreparedStatementSetter() {

                    public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {

                        ps.setString(1, code);
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

        String sql = "SELECT 1 FROM rdm_sync.version WHERE code = :code FOR UPDATE";
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

        String sql = String.format("  FROM %s %n WHERE %s = :state %n",
                localDataCriteria.getSchemaTable(), addDoubleQuotes(RDM_SYNC_INTERNAL_STATE_COLUMN));
        Map<String, Object> args = new HashMap<>();
        args.put("state", localDataCriteria.getState().name());
        if(localDataCriteria.getDeleted() != null) {
            if(Boolean.TRUE.equals(localDataCriteria.getDeleted().isDeleted())) {
                sql += " AND " + addDoubleQuotes(localDataCriteria.getDeleted().getFieldName()) + " = true";
            } else {
                sql += " AND coalesce(" + addDoubleQuotes(localDataCriteria.getDeleted().getFieldName()) + ", false) = false";
            }
        }

        MultivaluedMap<String, Object> filters = localDataCriteria.getFilters();
        if (filters != null) {
            args.putAll(filters);

            sql += filters.keySet().stream()
                    .map(key -> "   AND " + addDoubleQuotes(key) + " IN (:" + key + ")")
                    .collect(joining("\n"));
        }

        Integer count = namedParameterJdbcTemplate.queryForObject("SELECT count(*) \n" + sql, args, Integer.class);
        if (count == null || count == 0)
            return Page.empty();

        String pk = localDataCriteria.getPk();
        int limit = localDataCriteria.getLimit();
        sql += String.format(" ORDER BY %s %n LIMIT %d OFFSET %d", addDoubleQuotes(pk), limit, localDataCriteria.getOffset());
        var wrap = new Object() {
            int internalStateColumnIndex = -1;
        };

        List<Map<String, Object>> result = namedParameterJdbcTemplate.query("SELECT * \n" + sql,
                args, (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    if (wrap.internalStateColumnIndex == -1) {
                        wrap.internalStateColumnIndex = getInternalStateColumnIdx(rs.getMetaData(), localDataCriteria.getSchemaTable());
                    }

                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        if (i != wrap.internalStateColumnIndex) {
                            Object val = rs.getObject(i);
                            String key = rs.getMetaData().getColumnName(i);
                            map.put(key, val);
                        }
                    }

                    return map;
                });

        RestCriteria dataCriteria = new AbstractCriteria();
        dataCriteria.setPageNumber(localDataCriteria.getOffset() / limit);
        dataCriteria.setPageSize(limit);
        dataCriteria.setOrders(Sort.by(Sort.Order.asc(pk)).get().collect(Collectors.toList()));

        return new PageImpl<>(result, dataCriteria, count);
    }

    private int getInternalStateColumnIdx(ResultSetMetaData meta, String table) throws SQLException {

        for (int i = 1; i <= meta.getColumnCount(); i++) {

            if (meta.getColumnName(i).equals(RDM_SYNC_INTERNAL_STATE_COLUMN)) {
                return i;
            }
        }

        throw new RdmException("Internal state \"" + RDM_SYNC_INTERNAL_STATE_COLUMN + "\" column not found in " + table);
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

        String ddl = String.format("CREATE TABLE IF NOT EXISTS %s.%s (", schema, table);
        ddl += fieldMappings.stream()
                .map(mapping -> String.format("%s %s", mapping.getSysField(), mapping.getSysDataType()))
                .collect(Collectors.joining(", "));
        ddl += String.format(", %s BOOLEAN)", isDeletedFieldName);

        getJdbcTemplate().execute(ddl);
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
}
