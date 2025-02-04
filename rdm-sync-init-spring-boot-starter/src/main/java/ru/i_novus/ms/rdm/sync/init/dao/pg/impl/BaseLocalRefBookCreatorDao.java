package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
abstract class BaseLocalRefBookCreatorDao implements LocalRefBookCreatorDao {

    private static final String RDM_SYNC_INTERNAL_STATE_COLUMN = "rdm_sync_internal_local_row_state";

    private static final String LOCAL_ROW_STATE_UPDATE_FUNC = """
            CREATE OR REPLACE FUNCTION %1$s
              RETURNS trigger AS\s
            $BODY$\s
              BEGIN\s
                NEW.%2$s='%3$s';\s
                RETURN NEW;\s
              END;\s
            $BODY$ LANGUAGE 'plpgsql'\s
            """;

    private static final String INTERNAL_FUNCTION = "rdm_sync_internal_update_local_row_state()";


    protected final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    BaseLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    protected abstract void customizeTable(PgTable pgTable, VersionMapping mapping, List<FieldMapping> fieldMappings);

    /**
     *
     * @return Map колонок специфичных конкретному типу таблице, где ключи это наименования колонок, а значения их типы в таблице
     */
    protected abstract Map<String, String> getAdditionColumns(VersionMapping mapping);


    @Override
    public void createTable(String tableName,
                            String refBookCode,
                            VersionMapping mapping,
                            List<FieldMapping> fieldMappings,
                            String refDescription,
                            Map<String, String> fieldDescription) {
        if(tableExists(tableName)) {
            return;
        }
        Map<String, String> columns = getColumnsWithType(fieldMappings);
        columns.putAll(getAdditionColumns(mapping));
        PgTable pgTableWithColumns = new PgTable(tableName, refDescription, columns, fieldDescription, mapping.getPrimaryField(), mapping.getSysPkColumn());
        lockRefBookForUpdate(refBookCode);
        createSchemaIfNotExists(pgTableWithColumns.getSchema());
        createTable(pgTableWithColumns);
        customizeTable(pgTableWithColumns, mapping, fieldMappings);
        addInternalLocalRowStateColumnIfNotExists(tableName);
        createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        addInternalLocalRowStateUpdateTrigger(pgTableWithColumns);
    }

    private Map<String, String> getColumnsWithType(List<FieldMapping> fieldMappings) {
        return fieldMappings
                .stream()
                .collect(Collectors.toMap(FieldMapping::getSysField, FieldMapping::getSysDataType));
    }

    private void addInternalLocalRowStateUpdateTrigger(PgTable pgTable) {

        String triggerName = pgTable.getInternalLocalStateUpdateTriggerName();
        String schemaTable = pgTable.getName();

        final String sqlExists = "SELECT EXISTS(SELECT 1 FROM pg_trigger WHERE NOT tgisinternal AND tgname = :tgname)";
        Boolean exists = namedParameterJdbcTemplate.queryForObject(sqlExists, Map.of("tgname", triggerName.replaceAll("\"", "")), Boolean.class);

        if (Boolean.TRUE.equals(exists))
            return;

        final String sqlCreateFormat = """
                CREATE TRIGGER %s\s
                  BEFORE INSERT OR UPDATE\s
                  ON %s\s
                  FOR EACH ROW\s
                  EXECUTE PROCEDURE %s;""";
        String sqlCreate = String.format(sqlCreateFormat, triggerName, schemaTable, INTERNAL_FUNCTION);
        namedParameterJdbcTemplate.getJdbcTemplate().execute(sqlCreate);
    }

    private void addInternalLocalRowStateColumnIfNotExists(String schemaTable) {
        Map<String, String> params = new HashMap<>();
        params.putAll(getSchemaAndTableParams(schemaTable));
        params.put("internal_state_column", RDM_SYNC_INTERNAL_STATE_COLUMN);
        Boolean exists = namedParameterJdbcTemplate.queryForObject(
                """
                        SELECT EXISTS(SELECT 1 FROM information_schema.columns
                        WHERE table_schema = :schema AND table_name = :table AND column_name = :internal_state_column)
                        """,
                params,
                Boolean.class
        );
        if (Boolean.TRUE.equals(exists))
            return;

        PgTable pgTable = new PgTable(schemaTable);
        String query = String.format("ALTER TABLE %s ADD COLUMN %s VARCHAR NOT NULL DEFAULT '%s'", pgTable.getName(), RDM_SYNC_INTERNAL_STATE_COLUMN, "DIRTY");
        namedParameterJdbcTemplate.getJdbcTemplate().execute(query);

        query = String.format("CREATE INDEX ON %s (%s)", pgTable.getName(), RDM_SYNC_INTERNAL_STATE_COLUMN);
        namedParameterJdbcTemplate.getJdbcTemplate().execute(query);

        int n = namedParameterJdbcTemplate.update(String.format("UPDATE %s SET %s = :synced", pgTable.getName(), RDM_SYNC_INTERNAL_STATE_COLUMN), Map.of("synced", "SYNCED"));
        if (n != 0)
            log.info("{} records updated internal state to {} in table {}", n, "SYNCED", schemaTable);
    }

    public void createOrReplaceLocalRowStateUpdateFunction() {

        String sql = String.format(LOCAL_ROW_STATE_UPDATE_FUNC, INTERNAL_FUNCTION, RDM_SYNC_INTERNAL_STATE_COLUMN, "DIRTY");

        namedParameterJdbcTemplate.getJdbcTemplate().execute(sql);
    }


    @Override
    public Integer addMapping(VersionMapping versionMapping, List<FieldMapping> fieldMappings) {
        Integer mappingId = insertVersionMapping(versionMapping);
        insertFieldMapping(mappingId, fieldMappings);
        return mappingId;
    }

    @Override
    public void updateMapping(Integer oldMappingId, VersionMapping newVersionMapping, List<FieldMapping> fieldMappings) {
        updateCurrentMapping(newVersionMapping);
        insertFieldMapping(oldMappingId, fieldMappings);
    }

    @Override
    public List<String> getColumns(String tableName) {
        return namedParameterJdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = :schema AND table_name   = :table",
                getSchemaAndTableParams(tableName),
                String.class);
    }

    @Override
    public void refreshTable(String tableName,
                             List<FieldMapping> newFieldMappings,
                             String refDescription,
                             Map<String, String> fieldDescription) {
        PgTable pgTable = new PgTable(tableName, null,  getColumnsWithType(newFieldMappings), null, null, null);
        StringBuilder ddl = new StringBuilder(String.format("ALTER TABLE %s ", pgTable.getName()));


        ddl.append(
                pgTable.getColumns().orElseThrow().stream()
                .map(column -> String.format(" ADD COLUMN %s %s", column.name(), column.type()))
                .collect(Collectors.joining(", ")));

        namedParameterJdbcTemplate.getJdbcTemplate().execute(ddl.toString());
    }

    @Override
    public boolean tableExists(String tableName) {
        return namedParameterJdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT * FROM information_schema.tables  WHERE table_schema = :schema AND table_name = :table)",
                getSchemaAndTableParams(tableName),
                Boolean.class);
    }

    private Map<String, String> getSchemaAndTableParams(String tableName) {
        String schema = null;
        String table = null;
        if (tableName.contains(".")) {
            schema = tableName.split("\\.")[0];
            table = tableName.split("\\.")[1];
        } else {
            schema = "public";
            table = tableName;
        }
        return  Map.of("schema", schema, "table", table);
    }

    private void createSchemaIfNotExists(String schema) {
        namedParameterJdbcTemplate.getJdbcTemplate().execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schema));
    }

    private void createTable(PgTable tableWithColumns) {

        StringBuilder ddl = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (", tableWithColumns.getName()));

        ddl.append(
                tableWithColumns.getColumns()
                        .orElseThrow()
                        .stream()
                        .map(column -> String.format("%s %s", column.name(), column.type()))
                        .collect(Collectors.joining(", "))
        );
        ddl.append(");");

        ddl.append(String.format("COMMENT ON TABLE %s IS :tableDescription;", tableWithColumns.getName()));
        String columnCommentTemplate = "COMMENT ON COLUMN " + tableWithColumns.getName() + ".%s IS %s;";
        List<PgTable.Column> columns = new ArrayList<>(tableWithColumns.getColumns().orElseThrow());
        Map<String, String> params = new HashMap<>();
        params.put("tableDescription", tableWithColumns.getTableDescription().orElseThrow());
        for (int i = 0; i < columns.size(); i++) {
            String param = ":param" + i;
            ddl.append(String.format(columnCommentTemplate, columns.get(i).name(), param));
            params.put(param, columns.get(i).description());
        }
        namedParameterJdbcTemplate.update(ddl.toString(), params);
    }

    private boolean lockRefBookForUpdate(String code) {
        log.info("lock {} for update", code);
        String sql = "SELECT 1 FROM rdm_sync.refbook WHERE code = :code FOR UPDATE NOWAIT";
        try {
            namedParameterJdbcTemplate.queryForObject(sql, Map.of("code", code), Integer.class);
            log.info("Lock for refbook {} successfully acquired.", code);
            return true;

        } catch (CannotAcquireLockException ex) {
            log.info("Lock for refbook {} cannot be acquired.", code, ex);
            return false;
        }
    }

    Integer insertVersionMapping(VersionMapping versionMapping) {

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

        Integer refBookId = getSyncRefBookId(versionMapping.getCode());
        if(refBookId == null) {
            final String insRefSql = "insert into rdm_sync.refbook(code, name, source_id, sync_type, range) values(:code, :name, (SELECT id FROM rdm_sync.source WHERE code=:source_code), :type, :range)  RETURNING id";
            String refBookName = versionMapping.getRefBookName();
            Map<String, String> params = new HashMap<>(Map.of("code", versionMapping.getCode(), "name", refBookName != null ? refBookName : versionMapping.getCode(), "source_code", versionMapping.getSource(), "type", versionMapping.getType().name()));
            params.put("range", versionMapping.getRange() != null ? versionMapping.getRange().getRange() : null);
            refBookId = namedParameterJdbcTemplate.queryForObject(insRefSql,
                    params,
                    Integer.class);
        }

        Map<String, String> params = new HashMap(Map.of("refId", refBookId, "mappingId", mappingId));
        params.put("version", versionMapping.getRange() == null ? null : versionMapping.getRange().getRange());
        namedParameterJdbcTemplate.update("insert into rdm_sync.version(ref_id, mapping_id, version) values(:refId, :mappingId, :version)",
                params);

        return mappingId;
    }

    public Integer getSyncRefBookId(String code) {
        List<Integer> result = namedParameterJdbcTemplate.queryForList(
                "select id from rdm_sync.refbook where code = :code",
                Map.of("code", code),
                Integer.class
        );
        if (result.isEmpty())
            return null;

        return result.get(0);
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

    private void insertFieldMapping(Integer mappingId, List<FieldMapping> fieldMappings) {

        final String sqlDelete = "DELETE FROM rdm_sync.field_mapping WHERE mapping_id = ?";

        namedParameterJdbcTemplate.getJdbcTemplate().update(sqlDelete, mappingId);

        final String sqlInsert = """
                INSERT INTO rdm_sync.field_mapping\s
                      (mapping_id, sys_field, sys_data_type, rdm_field, ignore_if_not_exists, default_value, transform_expr)\s
                VALUES(?, ?, ?, ?, ?, ?, ?)""";

        namedParameterJdbcTemplate.getJdbcTemplate().batchUpdate(sqlInsert,
                new BatchPreparedStatementSetter() {

                    public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {

                        ps.setInt(1, mappingId);
                        ps.setString(2, fieldMappings.get(i).getSysField());
                        ps.setString(3, fieldMappings.get(i).getSysDataType());
                        ps.setString(4, fieldMappings.get(i).getRdmField());
                        ps.setBoolean(5, fieldMappings.get(i).getIgnoreIfNotExists());
                        ps.setString(6, fieldMappings.get(i).getDefaultValue());
                        ps.setString(7 ,fieldMappings.get(i).getTransformExpression());
                    }

                    public int getBatchSize() {
                        return fieldMappings.size();
                    }

                });
    }

    private void updateCurrentMapping(VersionMapping versionMapping) {

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

}
