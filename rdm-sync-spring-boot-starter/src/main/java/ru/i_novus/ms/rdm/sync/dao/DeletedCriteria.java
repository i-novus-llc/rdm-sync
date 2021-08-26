package ru.i_novus.ms.rdm.sync.dao;

public class DeletedCriteria {

    private final String fieldName;

    private final boolean deleted;

    public DeletedCriteria(String fieldName, boolean value) {
        this.fieldName = fieldName;
        this.deleted = value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
