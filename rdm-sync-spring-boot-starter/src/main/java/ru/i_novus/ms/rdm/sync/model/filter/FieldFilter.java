package ru.i_novus.ms.rdm.sync.model.filter;

import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

import java.util.List;

/**
 * Фильтр поля для поиска.
 */
public class FieldFilter {

    private final String field;

    private final DataTypeEnum type;

    private final List<FieldValueFilter> filters;

    public FieldFilter(String field, DataTypeEnum type, List<FieldValueFilter> filters) {

        this.field = field;
        this.type = type;
        this.filters = filters;
    }

    public String getField() {
        return field;
    }

    public DataTypeEnum getType() {
        return type;
    }

    public List<FieldValueFilter> getFilters() {
        return filters;
    }
}
