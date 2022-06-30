package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import java.io.Serializable;

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
}
