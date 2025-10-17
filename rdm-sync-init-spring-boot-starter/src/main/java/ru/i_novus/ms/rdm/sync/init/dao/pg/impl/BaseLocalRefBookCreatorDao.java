package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.PgTable;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
abstract class BaseLocalRefBookCreatorDao implements LocalRefBookCreatorDao {

    private static final String TABLE_COMMENT_TEMPLATE = "COMMENT ON TABLE %s IS %s;";
    private static final String COLUMN_COMMENT_TEMPLATE = "COMMENT ON COLUMN %s.%s IS %s;";


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
        Map<String, String> additionalColumns = getAdditionColumns(mapping);
        List<FieldMapping> fieldMappingsWithAdditional = new ArrayList<>();
        fieldMappingsWithAdditional.addAll(fieldMappings);
        fieldMappingsWithAdditional.addAll(getFieldMappings(additionalColumns));

        PgTable pgTableWithColumns = new PgTable(mapping, fieldMappingsWithAdditional, refDescription, fieldDescription);
        lockRefBookForUpdate(refBookCode);
        createSchemaIfNotExists(pgTableWithColumns.getSchema());
        createTable(pgTableWithColumns);
        customizeTable(pgTableWithColumns, mapping, fieldMappings);
    }

    private Map<String, String> getColumnsWithType(List<FieldMapping> fieldMappings) {
        return fieldMappings
                .stream()
                .collect(Collectors.toMap(FieldMapping::getSysField, FieldMapping::getSysDataType));
    }

    private List<FieldMapping> getFieldMappings(Map<String, String> columns) {
        return columns
                .entrySet()
                .stream()
                .map(entry -> new FieldMapping(entry.getKey(), entry.getValue(), null))
                .toList();
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
                             VersionMapping versionMapping,
                             List<FieldMapping> newFieldMappings,
                             String refDescription,
                             Map<String, String> fieldDescription) {
        PgTable pgTable = new PgTable(versionMapping, newFieldMappings, refDescription, fieldDescription);
        StringBuilder ddl = new StringBuilder(String.format("ALTER TABLE %s ", pgTable.getName()));


        ddl.append(
                pgTable.getColumns().orElseThrow().stream()
                .map(column -> String.format(" ADD COLUMN %s %s", column.name(), column.type()))
                .collect(Collectors.joining(", ")));

        ddl.append(";\n");
        concatColumnsComment(pgTable, ddl);

        namedParameterJdbcTemplate.getJdbcTemplate().execute(ddl.toString());
    }

    @Override
    public void addCommentsIfNotExists(String tableName,
                                       VersionMapping versionMapping,
                                       List<FieldMapping> fieldMappings,
                                       String refDescription,
                                       Map<String, String> columnDescriptions) {

        String[] splitTableName = tableName.split("\\.");
        String table;
        String schema;
        if (splitTableName.length == 1) {
            table = tableName;
            schema = "public";
        } else {
            table = splitTableName[1];
            schema = splitTableName[0];
        }

        String selectColumsWithNullableComment = """
            SELECT a.attname AS column_name
            FROM pg_attribute a
            JOIN pg_class c ON c.oid = a.attrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_description d ON d.objoid = a.attrelid AND d.objsubid = a.attnum
            WHERE c.relname = :table
              AND n.nspname = :schema
              AND a.attnum > 0
              AND NOT a.attisdropped
              AND d.description IS NULL;
            """;


        List<String> columnsWithNullableComment = namedParameterJdbcTemplate.queryForList(
                selectColumsWithNullableComment,
                Map.of("table", table, "schema", schema),
                String.class
        );
        List<FieldMapping> newFieldMappings = fieldMappings.stream()
                .filter(fm -> columnsWithNullableComment.contains(fm.getSysField())).toList();
        PgTable pgTable = new PgTable(versionMapping, newFieldMappings, refDescription, columnDescriptions);
        StringBuilder columnCommentQuery = new StringBuilder();
        concatColumnsComment(pgTable, columnCommentQuery);
        if (!columnCommentQuery.isEmpty()) {
            namedParameterJdbcTemplate.getJdbcTemplate().execute(columnCommentQuery.toString());
            log.info("refresh column comments for {}", pgTable.getName());
        }

        String existsTableCommentQuery = """
                SELECT EXISTS (
                                   SELECT 1
                                   FROM pg_class c
                                   JOIN pg_namespace n ON n.oid = c.relnamespace
                                   WHERE c.relname = :table
                                     AND n.nspname = :schema
                                     AND obj_description(c.oid) is not null
                               );
                """;
        Boolean existsTableComment = namedParameterJdbcTemplate.queryForObject(existsTableCommentQuery, Map.of("table", table, "schema", schema), Boolean.class);
        if (!existsTableComment) {
                pgTable.getTableDescription().ifPresent(description -> {
                    namedParameterJdbcTemplate.getJdbcTemplate().execute(
                            String.format(TABLE_COMMENT_TEMPLATE, pgTable.getName(), quoteLiteral(description))
                    );
                    log.info("refresh comment for {}", pgTable.getName());
                });
        }
    }

    @Override
    public boolean tableExists(String tableName) {
        return namedParameterJdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT * FROM information_schema.tables  WHERE table_schema = :schema AND table_name = :table)",
                getSchemaAndTableParams(tableName),
                Boolean.class);
    }

    private Map<String, String> getSchemaAndTableParams(String tableName) {
        String schema;
        String table;
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

        tableWithColumns.getTableDescription().ifPresent(description ->
            ddl.append(String.format("\n" + TABLE_COMMENT_TEMPLATE, tableWithColumns.getName(), quoteLiteral(description)))
        );
        concatColumnsComment(tableWithColumns, ddl);
        namedParameterJdbcTemplate.getJdbcTemplate().execute(ddl.toString());
    }

    private void concatColumnsComment(PgTable tableWithColumns, StringBuilder ddl) {
        tableWithColumns.getColumns().orElseThrow(() -> new IllegalArgumentException("At least one column required"))
                .stream()
                .filter(column -> column.description() != null)
                .forEach(column ->
                        ddl.append(String.format("\n" + COLUMN_COMMENT_TEMPLATE, tableWithColumns.getName(), column.name(), quoteLiteral(column.description())))
                );
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

        final String insMappingSql = """
                insert into rdm_sync.mapping (
                    deleted_field,
                    mapping_version,
                    sys_table,
                    sys_pk_field,
                    unique_sys_field,        match_case,    refreshable_range)
                values (
                    :deleted_field,
                    :mapping_version,
                    :sys_table,
                    :sys_pk_field,
                    :unique_sys_field,    :match_case,    :refreshable_range) RETURNING id""";

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
                      (mapping_id, sys_field, sys_data_type, rdm_field, ignore_if_not_exists, default_value, transform_expr, comment)\s
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)""";

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
                        ps.setString(8 ,fieldMappings.get(i).getComment());
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
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("code", versionMapping.getCode());
        updateParams.put("source_code", versionMapping.getSource());
        updateParams.put("sync_type", versionMapping.getType().toString());
        updateParams.put("name", versionMapping.getRefBookName());
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

    private static String quoteLiteral(String literal) {
        return literal == null ? "NULL" : ("'" + literal.replace("'", "''") + "'");
    }

}
