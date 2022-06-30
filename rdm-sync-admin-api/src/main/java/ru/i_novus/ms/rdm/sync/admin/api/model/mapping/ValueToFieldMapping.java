package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import java.io.Serializable;

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
}
