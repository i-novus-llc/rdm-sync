package ru.i_novus.ms.rdm.sync.model.filter;

import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Фильтр значений поля для поиска.
 */
public class FieldValueFilter {

    private final FilterTypeEnum type;

    private final List<Object> values;

    public FieldValueFilter(FilterTypeEnum type, List<Object> values) {

        this.type = type;
        this.values = values;
    }

    public FieldValueFilter(Map.Entry<FilterTypeEnum, List<Object>> entry) {

        this(entry.getKey(), entry.getValue());
    }

    public FilterTypeEnum getType() {
        return type;
    }

    public List<Object> getValues() {
        return values;
    }

    public boolean isEmpty() {
        return type == null || CollectionUtils.isEmpty(values);
    }
}
