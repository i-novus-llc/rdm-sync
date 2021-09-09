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
 * Критерий поиска справочников в источнике.
 */
@Getter
@Setter
public class SyncSourceVersionCriteria extends AbstractCriteria {

    @ApiParam("Код (идентификатор) источника")
    @QueryParam("sourceCode")
    private String sourceCode;

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Версия")
    @QueryParam("version")
    private String version;

    @ApiParam("Текст для поиска по нескольким полям")
    @QueryParam("text")
    private String text;

    public SyncSourceVersionCriteria() {
        // Nothing to do.
    }

    @JsonIgnore
    public boolean isEmpty() {

        return StringUtils.isEmpty(code) && StringUtils.isEmpty(version) && StringUtils.isEmpty(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncSourceVersionCriteria that = (SyncSourceVersionCriteria) o;
        return Objects.equals(sourceCode, that.sourceCode) &&
                Objects.equals(code, that.code) &&
                Objects.equals(version, that.version) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCode, code, version, text);
    }
}
