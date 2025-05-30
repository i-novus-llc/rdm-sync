package ru.i_novus.ms.rdm.sync.api.model;

import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PgTable {

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

    private Optional<Set<Column>> columns = Optional.empty();

    private Optional<String> primaryField = Optional.empty();

    private Optional<String> sysPkColumn = Optional.empty();

    private Optional<String> tableDescription = Optional.empty();

    private final String internalLocalStateUpdateTriggerName;

    private final String versionsTable;

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
            this.versionsTable = escapeName(splitedSchema + "." + splitedTable + "_versions");
        } else {
            this.table = escapeName(table);
            this.schema = "public";
            this.uniqueConstraint =  escapeName(table + "_uq");
            this.pkConstraint = escapeName(table + "_pk");
            this.internalLocalStateUpdateTriggerName = escapeName(schema + "_" + table + "_intrnl_lcl_rw_stt_updt");
            this.loadedVersionFk = escapeName(table + "_" + loadedVersionColumn + "_fk");
            this.versionsTable = escapeName(this.schema + "." + table + "_versions");
        }
    }

    public PgTable(String table,
                   String tableDescription,
                   Map<String, String> columns,
                   Map<String, String> columnDescriptions,
                   String primaryField,
                   String sysPkColumn) {
        this(table);
        this.tableDescription = Optional.ofNullable(tableDescription);
        this.tableDescription.ifPresent(this::validate);
        this.columns = Optional.of(
                columns.entrySet()
                        .stream()
                        .map(entry -> {
                            validate(entry.getValue());
                            String description = columnDescriptions.get(entry.getKey());
                            if (description != null) {
                                validate(description);
                            }
                            return new Column(escapeName(entry.getKey()), entry.getValue(), description);
                        }).collect(Collectors.toSet())
        );
        this.primaryField = Optional.ofNullable(escapeName(primaryField));
        this.sysPkColumn = Optional.ofNullable(escapeName(sysPkColumn));
    }

    public PgTable(VersionMapping versionMapping, List<FieldMapping> fieldMappings, String tableDescription, Map<String, String> columnDescriptions) {
        this(versionMapping.getTable());
        this.tableDescription = Optional.ofNullable(tableDescription);
        this.tableDescription.ifPresent(this::validate);
        this.columns = Optional.of(
                fieldMappings
                        .stream()
                        .map(fieldMapping -> {
                            validate(fieldMapping.getSysDataType());
                            String description = columnDescriptions.get(fieldMapping.getSysField());
                            if (description != null) {
                                validate(description);
                            }
                            return new Column(escapeName(fieldMapping.getSysField()), fieldMapping.getSysDataType(), description);
                        }).collect(Collectors.toSet())
        );
        this.primaryField = Optional.ofNullable(escapeName(versionMapping.getPrimaryField()));
        this.sysPkColumn = Optional.ofNullable(escapeName(versionMapping.getSysPkColumn()));
    }

    private String escapeName(String name) {
        if(name == null) {
            return null;
        }
        validate(name);
        if (name.contains(".")) {
            String firstPart = escapeName(name.split("\\.")[0]);
            String secondPart = escapeName(name.split("\\.")[1]);
            return firstPart + "." + secondPart;
        }
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

    public Optional<String> getTableDescription() {
        return tableDescription;
    }

    public String getVersionsTable() {
        return versionsTable;
    }

    public record Column(String name, String type, String description) {}
}
