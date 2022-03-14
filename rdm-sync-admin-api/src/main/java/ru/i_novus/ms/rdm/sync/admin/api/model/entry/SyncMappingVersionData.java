package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Информация о версиях, к которым применим маппинг.
 */
@Getter
@Setter
public class SyncMappingVersionData implements Serializable {

    private static final String LIST_DELIMITER = ", ";
    private static final String RANGE_DELIMITER = " - ";
    private static final String RANGE_UNLIMITED_START_VERSION = "…";
    private static final String RANGE_UNLIMITED_END_VERSION = "…";

    /** Тип указания версий, к которым применим маппинг. */
    private SyncMappingVersionTypeEnum type;

    /** Версии, к которым применим маппинг. */
    private List<String> versions;

    /** Текст с указанием версий. */
    private String text;

    /** Текст с указанием версий в зависимости от содержимого полей. */
    @JsonIgnore
    public String getVersionText() {

        if (!StringUtils.isEmpty(text))
            return text;

        if (type == null || CollectionUtils.isEmpty(versions))
            return null;

        return switch (type) {
            case UNIQUE -> versions.get(0);
            case LIST -> String.join(LIST_DELIMITER, versions);
            case RANGE -> getRangeText();
        };
    }

    /** Текст с указанием версий в случае диапазона. */
    @JsonIgnore
    private String getRangeText() {

        return getRangeVersion(versions.get(0), RANGE_UNLIMITED_START_VERSION) +
                RANGE_DELIMITER +
                getRangeVersion(versions.size() > 1 ? versions.get(1) : null, RANGE_UNLIMITED_END_VERSION);
    }

    /** Версия (номер) с учётом его отсутствия в случае диапазона. */
    @JsonIgnore
    private String getRangeVersion(String version, String unlimited) {
        return StringUtils.isEmpty(version) ? unlimited : version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncMappingVersionData that = (SyncMappingVersionData) o;
        return type == that.type &&
                Objects.equals(versions, that.versions) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, versions, text);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
