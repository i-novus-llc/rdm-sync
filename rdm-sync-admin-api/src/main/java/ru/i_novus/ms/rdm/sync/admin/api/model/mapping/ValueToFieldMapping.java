package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import java.io.Serializable;
import java.util.Objects;

/**
 * Маппинг на новое поле со значением.
 */
public class ValueToFieldMapping<T extends Serializable> extends SyncToFieldMapping {

    private T newValue;

    @SuppressWarnings("WeakerAccess")
    public ValueToFieldMapping(String newField, T newValue) {

        super(newField);

        this.newValue = newValue;
    }

    public T getNewValue() {
        return newValue;
    }

    public void setNewValue(T newValue) {
        this.newValue = newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ValueToFieldMapping<?> that = (ValueToFieldMapping<?>) o;
        return Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), newValue);
    }
}
