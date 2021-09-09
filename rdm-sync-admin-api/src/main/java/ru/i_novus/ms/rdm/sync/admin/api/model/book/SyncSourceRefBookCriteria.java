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
public class SyncSourceRefBookCriteria extends AbstractCriteria {

    @ApiParam("Код (идентификатор) источника")
    @QueryParam("sourceCode")
    private String sourceCode;

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Наименование справочника")
    @QueryParam("name")
    private String name;

    @ApiParam("Версия")
    @QueryParam("version")
    private String version;

    @ApiParam("Текст для поиска по нескольким полям")
    @QueryParam("text")
    private String text;

    @ApiParam("Наличие записи о синхронизации")
    @QueryParam("hasEntry")
    private Boolean hasEntry;

    public SyncSourceRefBookCriteria() {
        // Nothing to do.
    }

    /**
     * Проверка на отсутствие заполненности полей, ключевых для поиска.
     *
     * @return Результат проверки
     */
    @JsonIgnore
    public boolean isEmpty() {

        return StringUtils.isEmpty(code) && StringUtils.isEmpty(text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncSourceRefBookCriteria that = (SyncSourceRefBookCriteria) o;
        return Objects.equals(sourceCode, that.sourceCode) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(text, that.text) &&
                Objects.equals(hasEntry, that.hasEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCode, code, name, version, text, hasEntry);
    }
}
