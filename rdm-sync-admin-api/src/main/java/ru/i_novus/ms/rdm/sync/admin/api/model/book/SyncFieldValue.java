package ru.i_novus.ms.rdm.sync.admin.api.model.book;

import ru.i_novus.ms.rdm.sync.admin.api.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

public class SyncFieldValue<T extends Serializable> implements Serializable {

    private String field;
    private T value;

    @SuppressWarnings("unused")
    public SyncFieldValue() {
    }

    public SyncFieldValue(String field, T value) {

        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isNull() {
        return Objects.isNull(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncFieldValue<?> that = (SyncFieldValue<?>) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, value);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
