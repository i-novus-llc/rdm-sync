package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import jakarta.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска источников для синхронизации.
 */
@Getter
@Setter
public class SyncEntrySourceCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор источника")
    @QueryParam("id")
    private String id;

    @ApiParam("Код источника")
    @QueryParam("code")
    private String code;

    @ApiParam("Наименование (полное)")
    @QueryParam("name")
    private String name;

    @ApiParam("Надпись (краткое наименование)")
    @QueryParam("caption")
    private String caption;

    public SyncEntrySourceCriteria() {
        // Nothing to do.
    }

    @JsonIgnore
    public boolean isEmpty() {

        return StringUtils.isEmpty(id) &&
                StringUtils.isEmpty(code) &&
                StringUtils.isEmpty(name) &&
                StringUtils.isEmpty(caption);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntrySourceCriteria that = (SyncEntrySourceCriteria) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(caption, that.caption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, caption);
    }
}
