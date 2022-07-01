package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import java.util.Objects;

/**
 * Маппинг старого поля на новое поле.
 */
public class FieldToFieldMapping extends SyncToFieldMapping {

    private String oldField;

    public FieldToFieldMapping(String newField, String oldField) {

        super(newField);
        
        this.oldField = oldField;
    }

    public String getOldField() {
        return oldField;
    }

    public void setOldField(String oldField) {
        this.oldField = oldField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FieldToFieldMapping that = (FieldToFieldMapping) o;
        return Objects.equals(oldField, that.oldField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), oldField);
    }
}
