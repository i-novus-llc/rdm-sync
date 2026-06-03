package ru.i_novus.ms.rdm.sync.dao.criteria;

import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;

import java.util.Collections;
import java.util.List;

public abstract class BaseDataCriteria {

    private final String schemaTable;
    private final String pk;
    private final int limit;
    private final int offset;
    private final List<FieldFilter> filters;
    private String filterSql;  //todo зарефакторить без явной завязки на sql
    private List<Sort.Order> sortOrders = Collections.emptyList();

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

    public List<Sort.Order> getSortOrders() {
        return sortOrders;
    }

    public void setSortOrders(List<Sort.Order> sortOrders) {
        this.sortOrders = sortOrders != null ? sortOrders : Collections.emptyList();
    }
}
