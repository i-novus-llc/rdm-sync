package ru.i_novus.ms.rdm.sync.dao.criteria;

import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;

import java.util.List;

public abstract class BaseDataCriteria {

    private final String schemaTable;
    private final String pk;
    private final int limit;
    private final int offset;
    private final List<FieldFilter> filters;
    private String filterSql;

    public BaseDataCriteria(String schemaTable, String pk, int limit, int offset, List<FieldFilter> filters) {

        this.schemaTable = schemaTable;
        this.pk = pk;
        this.limit = limit;
        this.offset = offset;
        this.filters = filters;
    }

    public String getSchemaTable() {
        return schemaTable;
    }

    public String getPk() {
        return pk;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public List<FieldFilter> getFilters() {
        return filters;
    }

    public String getFilterSql() {
        return filterSql;
    }

    public void setFilterSql(String filterSql) {
        this.filterSql = filterSql;
    }
}
