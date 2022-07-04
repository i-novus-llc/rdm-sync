package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * Подсказка по маппингу для версии записи.
 */
@Getter
public class SyncEntryMappingHint implements Serializable {

    private final String id;
    private final String name;
    private final String href;

    public SyncEntryMappingHint(String id, String name, String href) {

        this.id = id;
        this.name = name;
        this.href = href;
    }

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
