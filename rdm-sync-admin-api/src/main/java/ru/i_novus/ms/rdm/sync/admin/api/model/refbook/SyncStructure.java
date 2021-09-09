package ru.i_novus.ms.rdm.sync.admin.api.model.refbook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static java.util.stream.Collectors.toList;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncStructure implements Serializable {

    public static final SyncStructure EMPTY = new SyncStructure();

    private List<SyncField> fields;

    public SyncStructure() {

        this.fields = new ArrayList<>(0);
    }

    public SyncStructure(List<SyncField> fields) {

        this.fields = getOrCreateList(fields);
    }

    public SyncStructure(SyncStructure structure) {

        this.fields = copyList(structure.fields, SyncField::new);
    }

    public List<SyncField> getFields() {
        return fields;
    }

    public void setFields(List<SyncField> fields) {
        this.fields = getOrCreateList(fields);
    }

    public SyncField getField(String code) {

        if (CollectionUtils.isEmpty(fields))
            return null;

        return fields.stream()
                .filter(field -> field.getCode().equals(code))
                .findAny().orElse(null);
    }

    private static <T> List<T> getOrCreateList(List<T> list) {
        return list == null ? new ArrayList<>(0) : list;
    }

    private static <T> List<T> copyList(List<T> values, UnaryOperator<T> copy) {

        if (CollectionUtils.isEmpty(values))
            return new ArrayList<>(0);

        return values.stream().map(copy).collect(toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncStructure that = (SyncStructure) o;
        return Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
