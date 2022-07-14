package ru.i_novus.ms.rdm.sync.admin.api.model.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.refbook.SyncStructure;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель версии справочника из источника.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncSourceVersion implements Serializable {

    /** Идентификатор версии. */
    private String id;

    /** Идентификатор (код) справочника. */
    private String refBookId;

    /** Структура версии. */
    private SyncStructure structure;

    /** Код (справочника в) версии. */
    private String code;

    /** Наименование (справочника в) версии. */
    private String name;

    /** Версия (номер). */
    private String version;

    /** Комментарий (описание). */
    private String comment;

    /** Дата начала действия (публикации). */
    private LocalDateTime fromDate;

    /** Дата окончания действия. */
    private LocalDateTime toDate;

    /** Дата создания. */
    private LocalDateTime creationDate;

    /** Дата последнего действия. */
    private LocalDateTime lastActionDate;

    /** Признак наличия версии записи. */
    private Boolean isPresent;

    /** Отображаемое наименование. */
    private String displayName;

    /** Отображаемый номер версии. */
    private String displayVersion;

    public SyncSourceVersion() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncSourceVersion that = (SyncSourceVersion) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(refBookId, that.refBookId) &&
                Objects.equals(structure, that.structure) &&

                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(comment, that.comment) &&

                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(lastActionDate, that.lastActionDate) &&

                Objects.equals(isPresent, that.isPresent) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(displayVersion, that.displayVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, refBookId, structure, code, name, comment,
                fromDate, toDate, creationDate, lastActionDate,
                isPresent, displayName, displayVersion);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
