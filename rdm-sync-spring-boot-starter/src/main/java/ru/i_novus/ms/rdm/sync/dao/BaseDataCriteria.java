package ru.i_novus.ms.rdm.sync.dao;

import javax.ws.rs.core.MultivaluedMap;

public abstract class BaseDataCriteria {

    private final String schemaTable;
    private final String pk;
    private final int limit;
    private final int offset;
    private final MultivaluedMap<String, Object> filters;

    public BaseDataCriteria(String schemaTable, String pk, int limit, int offset, MultivaluedMap<String, Object> filters) {
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

    public MultivaluedMap<String, Object> getFilters() {
        return filters;
    }
}
