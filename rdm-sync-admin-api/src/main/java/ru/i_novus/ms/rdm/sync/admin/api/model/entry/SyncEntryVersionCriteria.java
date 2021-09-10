package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска версий записи о синхронизации справочников.
 */
@Getter
@Setter
public class SyncEntryVersionCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор версии записи")
    @QueryParam("id")
    private String id;

    @ApiParam("Идентификатор записи")
    @QueryParam("entryId")
    private String entryId;

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Версия (номер) справочника")
    @QueryParam("version")
    private String version;

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

        SyncEntryVersionCriteria that = (SyncEntryVersionCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(entryId, that.entryId) &&
                Objects.equals(code, that.code) &&
                Objects.equals(version, that.version) &&
                Objects.equals(text, that.text) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, entryId, code, version, text, count);
    }
}
