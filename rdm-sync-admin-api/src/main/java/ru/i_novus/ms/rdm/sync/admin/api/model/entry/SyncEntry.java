package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель записи о синхронизации справочника.
 */
@Getter
@Setter
public class SyncEntry implements Serializable {

    /** Идентификатор записи. */
    private String id;

    /** Код справочника. */
    private String code;

    /** Наименование справочника. */
    private String name;

    /** Код локального хранилища. */
    private String storageCode;

    /** Признак удаляемости. */
    private boolean removable;

    /** Версия (номер) справочника. */
    private String version;

    /** Поддержка версионности. */
    private boolean versioned;

    /** Признак автообновления. */
    private boolean autoUpdatable;

    /** Источник для обновления. */
    private SyncEntrySource source;

    /** Дата последнего обновления. */
    private LocalDateTime lastDateTime;

    /** Статус последнего обновления. */
    private String lastStatus;

    public SyncEntry() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntry that = (SyncEntry) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&

                Objects.equals(storageCode, that.storageCode) &&
                Objects.equals(removable, that.removable) &&

                Objects.equals(version, that.version) &&
                versioned == that.versioned &&
                autoUpdatable == that.autoUpdatable &&

                Objects.equals(source, that.source) &&
                Objects.equals(lastDateTime, that.lastDateTime) &&
                Objects.equals(lastStatus, that.lastStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name,
                storageCode, removable,
                version, versioned, autoUpdatable,
                source, lastDateTime, lastStatus);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
