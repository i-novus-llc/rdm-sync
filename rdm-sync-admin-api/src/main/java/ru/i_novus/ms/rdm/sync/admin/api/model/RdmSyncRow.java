package ru.i_novus.ms.rdm.sync.admin.api.model;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель записи о синхронизации справочника.
 */
@Getter
@Setter
public class RdmSyncRow implements Serializable {

    /** Код справочника. */
    private String code;

    /** Наименование справочника. */
    private String name;

    /** Версия (номер) справочника. */
    private String version;

    /** Признак версионности. */
    private boolean versioned;

    /** Признак автообновления. */
    private boolean autoUpdatable;

    /** Источник для обновления. */
    private String sourceType;

    /** Дата последнего обновления. */
    private LocalDateTime lastDateTime;

    /** Статус последнего обновления. */
    private String lastStatus;

    public RdmSyncRow() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RdmSyncRow that = (RdmSyncRow) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&

                Objects.equals(version, that.version) &&
                versioned == that.versioned &&
                autoUpdatable == that.autoUpdatable &&

                Objects.equals(sourceType, that.sourceType) &&
                Objects.equals(lastDateTime, that.lastDateTime) &&
                Objects.equals(lastStatus, that.lastStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name,
                version, versioned, autoUpdatable,
                sourceType, lastDateTime, lastStatus);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
