package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Модель маппинга для версии записи.
 * <p/>
 * Используется как при создании записи о синхронизации, так и при её изменении.
 */
@Getter
@Setter
public class SyncEntryMapping implements Serializable {

    /** Идентификатор маппинга. */
    private String id;

    /** Запись о синхронизации. */
    private SyncEntry entry;

    /** Версия записи о синхронизации. */
    private SyncEntryVersion version;

    /** Маппинг полей версии. */
    private List<SyncMappingField> mappingFields;

    /** Тип выполняемого действия. */
    private String actionType; // из критерия!

    public SyncEntryMapping() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntryMapping that = (SyncEntryMapping) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(entry, that.entry) &&
                Objects.equals(version, that.version) &&
                Objects.equals(mappingFields, that.mappingFields) &&
                Objects.equals(actionType, that.actionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entry, version, mappingFields, actionType);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
