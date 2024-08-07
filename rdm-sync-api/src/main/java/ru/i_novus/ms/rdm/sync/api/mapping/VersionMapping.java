package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.*;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class VersionMapping implements Serializable {

    private Integer id;
    private String code;
    private String refBookName;
    private String table;
    private String sysPkColumn;
    private String source;
    private String primaryField;
    private String deletedField;
    private LocalDateTime mappingLastUpdated;
    private int mappingVersion;
    private Integer mappingId;
    private SyncTypeEnum type;
    private Range range;
    private boolean matchCase = true;
    private boolean refreshableRange;

    public Range getRange() {
        return range;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getRefBookName() {
        return refBookName;
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

    public boolean isMatchCase() {
        return matchCase;
    }

    public boolean isRefreshableRange() {
        return refreshableRange;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean equalsByRange(VersionMapping other) {
        return Objects.equals(this.code, other.code) && Objects.equals( this.range, other.range);
    }


}

