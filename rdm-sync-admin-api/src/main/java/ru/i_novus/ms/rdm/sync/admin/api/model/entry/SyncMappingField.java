package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.refbook.SyncField;

import java.util.Objects;

/**
 * Поле (версии) справочника при маппинге сполем из источника.
 */
@Getter
@Setter
public class SyncMappingField extends SyncField {

    /** Код исходного поля (из источника). */
    private String originCode;

    public SyncMappingField() {
        // nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SyncMappingField that = (SyncMappingField) o;
        return Objects.equals(originCode, that.originCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), originCode);
    }
}
