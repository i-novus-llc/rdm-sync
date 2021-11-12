package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.admin.api.model.refbook.SyncField;

import java.util.Objects;

/**
 * Поле (версии) справочника при маппинге с полем из источника.
 */
@Getter
@Setter
public class SyncMappingField extends SyncField {

    /** Код исходного поля (из источника). */
    private String originCode;

    /** Тип исходного поля (из источника). */
    private String originType;

    /** Присутствие поля. */
    private boolean isPresent;

    public SyncMappingField() {
        // nothing to do.
    }

    /**
     * Проверка кода поля на присутствие в маппинге.
     *
     * @return Результат проверки
     */
    @JsonIgnore
    public boolean isCodePresent() {
        return !StringUtils.isEmpty(getCode());
    }

    /**
     * Проверка исходного поля на присутствие в маппинге.
     *
     * @return Результат проверки
     */
    @JsonIgnore
    public boolean isOriginPresent() {
        return !StringUtils.isEmpty(getOriginCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SyncMappingField that = (SyncMappingField) o;
        return Objects.equals(originCode, that.originCode) &&
                Objects.equals(originType, that.originType) &&
                isPresent == that.isPresent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), originCode, originType, isPresent);
    }
}
