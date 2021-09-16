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

    public SyncMappingField() {
        // nothing to do.
    }

    /**
     * Проверка поля на присутствие при маппинге.
     *
     * @return Результат проверки
     */
    @JsonIgnore
    public boolean isPresent() {
        return !StringUtils.isEmpty(getCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SyncMappingField that = (SyncMappingField) o;
        return Objects.equals(originCode, that.originCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), originCode);
    }
}
