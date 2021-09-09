package ru.i_novus.ms.rdm.sync.admin.api.model.source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска источников для синхронизации.
 */
@Getter
@Setter
public class SyncSourceCriteria extends AbstractCriteria {

    @ApiParam("Код (идентификатор) источника")
    @QueryParam("code")
    private String code;

    @ApiParam("Наименование (полное)")
    @QueryParam("name")
    private String name;

    @ApiParam("Надпись (краткое наименование)")
    @QueryParam("caption")
    private String caption;

    public SyncSourceCriteria() {
        // Nothing to do.
    }

    @JsonIgnore
    public boolean isEmpty() {

        return StringUtils.isEmpty(code) &&
                StringUtils.isEmpty(name) &&
                StringUtils.isEmpty(caption);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncSourceCriteria that = (SyncSourceCriteria) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(caption, that.caption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, caption);
    }
}
