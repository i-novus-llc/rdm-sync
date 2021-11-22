package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * Модель источника для синхронизации.
 */
@Getter
@Setter
public class SyncEntrySource implements Serializable {

    /** Идентификатор. */
    private String id;

    /** Код. */
    private String code;

    /** Наименование (полное). */
    private String name;

    /** Надпись (краткое наименование). */
    private String caption;

    /** Наименование сервиса для доступа. */
    private String service;

    /** Параметры сервиса для доступа. */
    private String params;

    public SyncEntrySource() {
        // Nothing to do.
    }

    @JsonIgnore
    public boolean isEmpty() {

        return StringUtils.isEmpty(id) &&
                StringUtils.isEmpty(code) &&

                StringUtils.isEmpty(name) &&
                StringUtils.isEmpty(caption) &&

                StringUtils.isEmpty(service) &&
                StringUtils.isEmpty(params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntrySource that = (SyncEntrySource) o;
        return Objects.equals(id, that.code) &&
                Objects.equals(code, that.code) &&

                Objects.equals(name, that.name) &&
                Objects.equals(caption, that.caption) &&

                Objects.equals(service, that.service) &&
                Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, caption, service, params);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
