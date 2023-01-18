package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
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

    /** Дополнительные коды. */
    private List<String> otherCodes;

    /** Код локального хранилища. */
    private String storageCode;

    /** Признак удаляемости. */
    private boolean removable;

    /** Идентификатор версии справочника. */
    private String versionId;

    /** Версия (номер версии) справочника. */
    private String version;

    /** Поддержка версионности. */
    private boolean versioned;

    /** Признак автообновления. */
    private boolean autoUpdatable;

    /** Источник для обновления. */
    private SyncEntrySource source;

    /** Наличие версии в системе. */
    private boolean isPresent;

    /** Версия последнего обновления. */
    private String lastVersion;

    /** Дата последнего обновления. */
    private LocalDateTime lastDateTime;

    /** Статус последнего обновления. */
    private String lastStatus;

    /** Отображаемое наименование справочника. */
    private String displayName;

    /** Отображаемый номер версии. */
    private String displayVersion;

    private Boolean existsInExternal;

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
                Objects.equals(otherCodes, that.otherCodes) &&

                Objects.equals(storageCode, that.storageCode) &&
                Objects.equals(removable, that.removable) &&

                Objects.equals(versionId, that.versionId) &&
                Objects.equals(version, that.version) &&

                versioned == that.versioned &&
                autoUpdatable == that.autoUpdatable &&
                Objects.equals(source, that.source) &&

                Objects.equals(isPresent, that.isPresent) &&
                Objects.equals(lastVersion, that.lastVersion) &&
                Objects.equals(lastDateTime, that.lastDateTime) &&
                Objects.equals(lastStatus, that.lastStatus) &&

                Objects.equals(displayName, that.displayName) &&
                Objects.equals(displayVersion, that.displayVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, otherCodes,
                storageCode, removable,
                versionId, version, versioned, autoUpdatable, source,
                isPresent, lastVersion, lastDateTime, lastStatus,
                displayName, displayVersion);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
