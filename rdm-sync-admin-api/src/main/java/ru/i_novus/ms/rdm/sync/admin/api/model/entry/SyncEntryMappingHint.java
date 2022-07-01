package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * Подсказка по маппингу для версии записи.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncEntryMappingHint implements Serializable {

    private String id;
    private String name;
    private String href;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntryMappingHint that = (SyncEntryMappingHint) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(href, that.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, href);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
