package ru.i_novus.ms.rdm.sync.admin.api.model.refbook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Поле (версии) справочника.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncField implements Serializable {

    /** Код поля (наименование колонки). */
    private String code;

    /** Наименование поля. */
    private String name;

    /** Тип поля. */
    private String type;

    /** Признак первичного ключа. */
    private boolean isPrimary;

    /** Описание поля. */
    private String description;

    /** Другие параметры. */
    @Setter(AccessLevel.NONE)
    private final Map<String, Serializable> params = new HashMap<>();

    public SyncField() {
        // Nothing to do.
    }

    public SyncField(SyncField field) {

        this.code = field.code;
        this.name = field.name;
        this.type = field.type;

        this.isPrimary = field.isPrimary;
        this.description = field.description;

        this.params.putAll(field.params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncField that = (SyncField) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&

                Objects.equals(isPrimary, that.isPrimary) &&
                Objects.equals(description, that.description) &&
                Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, type, isPrimary, description, params);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
