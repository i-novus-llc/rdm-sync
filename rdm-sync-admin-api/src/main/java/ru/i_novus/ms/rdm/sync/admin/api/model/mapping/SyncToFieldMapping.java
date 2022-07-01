package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * Маппинг на новое поле.
 */
public class SyncToFieldMapping implements Serializable {

    private String newField;

    public SyncToFieldMapping() {
        // nothing to do.
    }

    public SyncToFieldMapping(String newField) {
        this.newField = newField;
    }

    public String getNewField() {
        return newField;
    }

    public void setNewField(String newField) {
        this.newField = newField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncToFieldMapping that = (SyncToFieldMapping) o;
        return Objects.equals(newField, that.newField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newField);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
