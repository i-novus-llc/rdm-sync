package ru.i_novus.ms.rdm.sync.admin.api.model.book;

import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска справочников в источнике.
 */
@Getter
@Setter
public class SyncRefBookCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор записи")
    @QueryParam("id")
    private String id;

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Наименование справочника")
    @QueryParam("name")
    private String name;

    public SyncRefBookCriteria() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncRefBookCriteria that = (SyncRefBookCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name);
    }
}
