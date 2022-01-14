package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class VersionMapping {

    private Integer id;
    private String code;
    private String refBookName;
    private String version;
    private String table;
    private String sysPkColumn;
    private String source;
    private String primaryField;
    private String deletedField;
    private LocalDateTime mappingLastUpdated;
    private int mappingVersion;
    private Integer mappingId;
    private SyncTypeEnum type;

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getRefBookName() {
        return refBookName;
    }

    public String getVersion() {
        return version;
    }

    public String getTable() {
        return table;
    }

    public String getSysPkColumn() {
        return Objects.toString(sysPkColumn, "_sync_rec_id");
    }

    public String getSource() {
        return source;
    }

    public String getPrimaryField() {
        return primaryField;
    }

    public String getDeletedField() {
        return deletedField;
    }

    public LocalDateTime getMappingLastUpdated() {
        return mappingLastUpdated;
    }

    public int getMappingVersion() {
        return mappingVersion;
    }

    public Integer getMappingId() {
        return mappingId;
    }

    public SyncTypeEnum getType() {
        return type;
    }
}

