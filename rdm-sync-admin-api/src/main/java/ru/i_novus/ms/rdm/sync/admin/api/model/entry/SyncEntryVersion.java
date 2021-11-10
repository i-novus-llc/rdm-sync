package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель версии записи о синхронизации справочника.
 */
@Getter
@Setter
public class SyncEntryVersion implements Serializable {

    /** Идентификатор версии. */
    private String id;

    /** Идентификатор записи. */
    private String entryId;

    /** Код (справочника в) версии. */
    private String code;

    /** Наименование (справочника в) версии. */
    private String name;

    /** Версия (номер) справочника. */
    private String version;

    /** Дата начала действия. */
    private LocalDateTime fromDate;

    /** Дата окончания действия. */
    private LocalDateTime toDate;

    /** Наличие в системе. */
    private boolean isPresent;

    /** Дата последнего обновления. */
    private LocalDateTime lastDateTime;

    /** Статус последнего обновления. */
    private String lastStatus;

    /** Отображаемое наименование. */
    private String displayName;

    public SyncEntryVersion() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntryVersion that = (SyncEntryVersion) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(entryId, that.entryId) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&

                Objects.equals(version, that.version) &&
                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate) &&

                Objects.equals(isPresent, that.isPresent) &&
                Objects.equals(lastDateTime, that.lastDateTime) &&
                Objects.equals(lastStatus, that.lastStatus) &&

                Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entryId, code, name,
                version, fromDate, toDate,
                isPresent, lastDateTime, lastStatus, displayName);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
