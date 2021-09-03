package ru.i_novus.ms.rdm.sync.admin.api.model.book;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class SyncRefBook implements Serializable {

    private String id;

    private String code;

    private String name;

    private SyncStructure structure;

    public SyncRefBook() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncRefBook that = (SyncRefBook) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(structure, that.structure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, structure);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
