package ru.i_novus.ms.rdm.sync.admin.api.model.book;

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
public class SyncVersionCriteria extends AbstractCriteria {

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Версия")
    @QueryParam("version")
    private String version;

    @ApiParam("Текст для поиска по нескольким полям")
    @QueryParam("text")
    private String text;

    @ApiParam("Код (идентификатор) источника")
    @QueryParam("sourceCode")
    private String sourceCode;

    public SyncVersionCriteria() {
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

        SyncVersionCriteria that = (SyncVersionCriteria) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(version, that.version) &&
                Objects.equals(text, that.text) &&
                Objects.equals(sourceCode, that.sourceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, version, text, sourceCode);
    }
}
