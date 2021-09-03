package ru.i_novus.ms.rdm.sync.admin.api.model.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Модель справочника для синхронизации.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncRefBook extends SyncVersion {

    /** Отображаемое наименование. */
    private String displayName;

    public SyncRefBook() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SyncRefBook that = (SyncRefBook) o;
        return Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), displayName);
    }
}
