package ru.i_novus.ms.rdm.sync.admin.n2o.model;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.book.SyncRefBook;
import ru.i_novus.ms.rdm.sync.admin.api.model.book.SyncStructure;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * Запрос на формирование метаданных в SyncMapping*Provider'ах.
 */
@Getter
@Setter
public class SyncMappingRequest implements Serializable {

    /** Код (идентификатор) источника. */
    private String sourceCode;

    /** Код справочника. */
    private String code;

    /** Версия (номер). */
    private String version;

    /** Тип выполняемого действия. */
    private String actionType;

    /** Справочник. */
    private SyncRefBook refBook;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncMappingRequest that = (SyncMappingRequest) o;
        return Objects.equals(sourceCode, that.sourceCode) &&
                Objects.equals(code, that.code) &&
                Objects.equals(version, that.version) &&
                Objects.equals(actionType, that.actionType) &&
                Objects.equals(refBook, that.refBook);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCode, code, version, actionType, refBook);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
