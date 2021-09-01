package ru.i_novus.ms.rdm.sync.admin.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.admin.api.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * Модель источника для синхронизации.
 */
@Getter
@Setter
public class RdmSyncSource implements Serializable {

    /** Код (идентификатор). */
    private String code;

    /** Наименование (полное). */
    private String name;

    /** Надпись (краткое наименование). */
    private String caption;

    /** Ссылка на источник. */
    private String link;

    /** Токен аутентификации. */
    private String token;

    public RdmSyncSource() {
        // Nothing to do.
    }

    @JsonIgnore
    public boolean isEmpty() {

        return StringUtils.isEmpty(code) &&
                StringUtils.isEmpty(name) &&
                StringUtils.isEmpty(caption) &&

                StringUtils.isEmpty(link) &&
                StringUtils.isEmpty(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RdmSyncSource that = (RdmSyncSource) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(caption, that.caption) &&

                Objects.equals(link, that.link) &&
                Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, caption, link, token);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
