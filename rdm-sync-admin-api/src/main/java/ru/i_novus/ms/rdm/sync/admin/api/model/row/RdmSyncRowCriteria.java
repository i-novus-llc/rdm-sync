package ru.i_novus.ms.rdm.sync.admin.api.model.row;

import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска записей о синхронизации справочников.
 */
@Getter
@Setter
public class RdmSyncRowCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор записи")
    @QueryParam("id")
    private String id;

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Наименование справочника")
    @QueryParam("name")
    private String name;

    @ApiParam("Текст для поиска по нескольким полям")
    @QueryParam("text")
    private String text;

    @ApiParam("Общее количество записей")
    @QueryParam("count")
    private Integer count;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RdmSyncRowCriteria that = (RdmSyncRowCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(text, that.text) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, code, name, text, count);
    }
}
