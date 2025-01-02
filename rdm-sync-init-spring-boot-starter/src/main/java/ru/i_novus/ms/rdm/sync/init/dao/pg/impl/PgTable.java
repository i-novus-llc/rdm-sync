package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class PgTable {

    // Регулярное выражение для поиска потенциальных SQL-инъекций
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(.*(--|#|/\\*|\\*\\/|;|\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b).*)",
            Pattern.CASE_INSENSITIVE);

    private final String table;

    private final String schema;

    private final String uniqueConstraint;

    private final String pkConstraint;

    private final String loadedVersionColumn = "version_id";

    private final String loadedVersionFk;

    private Optional<Set<Column>> columns = Optional.empty();;

    private Optional<String> primaryField = Optional.empty();

    private Optional<String> sysPkColumn = Optional.empty();

    private final String internalLocalStateUpdateTriggerName;

    public PgTable(String table) {
        if (table.contains(".")) {
            String splitedSchema = table.split("\\.")[0];
            String splitedTable = table.split("\\.")[1];
            this.schema = escapeName(splitedSchema);
            this.table = escapeName(splitedTable);
            this.uniqueConstraint =  escapeName(splitedTable + "_uq");
            this.pkConstraint = escapeName(splitedTable + "_pk");
            this.loadedVersionFk = escapeName(splitedTable + "_" + loadedVersionColumn + "_fk");
            this.internalLocalStateUpdateTriggerName = escapeName(splitedSchema + "_" + splitedTable + "_intrnl_lcl_rw_stt_updt");
        } else {
            this.table = escapeName(table);
            this.schema = "public";
            this.uniqueConstraint =  escapeName(table + "_uq");
            this.pkConstraint = escapeName(table + "_pk");
            this.internalLocalStateUpdateTriggerName = escapeName(schema + "_" + table + "_intrnl_lcl_rw_stt_updt");
            this.loadedVersionFk = escapeName(table + "_" + loadedVersionColumn + "_fk");
        }
    }

    public PgTable(String table, Map<String, String> columns, String primaryField, String sysPkColumn) {
        this(table);
        this.columns = Optional.of(
                columns.entrySet()
                        .stream()
                        .map(entry -> {
                            validate(entry.getValue());
                            return new Column(escapeName(entry.getKey()), entry.getValue());
                        }).collect(Collectors.toSet())
        );
        this.primaryField = Optional.ofNullable(escapeName(primaryField));
        this.sysPkColumn = Optional.ofNullable(escapeName(sysPkColumn));
    }

    private String escapeName(String name) {
        if(name == null) {
            return null;
        }
        validate(name);
        return "\"" + name + "\"";
    }

    private void validate(String name) {
        if ( SQL_INJECTION_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(name + "illegal value");
        }
    }

    public String getTable() {
        return table;
    }

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return getSchema() + "." + getTable();
    }

    public Optional<Set<Column>> getColumns() {
        return columns;
    }

    public String getUniqueConstraint() {
        return uniqueConstraint;
    }

    public Optional<String> getPrimaryField() {
        return primaryField;
    }

    public String getInternalLocalStateUpdateTriggerName() {
        return internalLocalStateUpdateTriggerName;
    }

    public String getPkConstraint() {
        return pkConstraint;
    }

    public Optional<String> getSysPkColumn() {
        return sysPkColumn;
    }

    public String getLoadedVersionColumn() {
        return loadedVersionColumn;
    }

    public String getLoadedVersionFk() {
        return loadedVersionFk;
    }

    public static record Column(String name, String type) {}
}
