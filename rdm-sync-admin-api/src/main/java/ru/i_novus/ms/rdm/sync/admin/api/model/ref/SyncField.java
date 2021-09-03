package ru.i_novus.ms.rdm.sync.admin.api.model.ref;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import ru.i_novus.ms.rdm.sync.admin.api.JsonUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncField implements Serializable {

    private String code;

    private String name;

    private String type;

    private Boolean isPrimary = Boolean.FALSE;

    private String description;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary != null && isPrimary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Serializable> getParams() {
        return params;
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
