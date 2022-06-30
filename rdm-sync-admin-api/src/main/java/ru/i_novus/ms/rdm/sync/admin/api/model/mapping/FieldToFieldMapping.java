package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

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
}
