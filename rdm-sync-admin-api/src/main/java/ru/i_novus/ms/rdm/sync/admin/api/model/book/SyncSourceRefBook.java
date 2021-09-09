package ru.i_novus.ms.rdm.sync.admin.api.model.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Модель справочника из источника.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncSourceRefBook extends SyncSourceVersion {

    /** Возможность удаления. */
    private Boolean removable;

    public SyncSourceRefBook() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SyncSourceRefBook that = (SyncSourceRefBook) o;
        return Objects.equals(removable, that.removable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), removable);
    }
}
