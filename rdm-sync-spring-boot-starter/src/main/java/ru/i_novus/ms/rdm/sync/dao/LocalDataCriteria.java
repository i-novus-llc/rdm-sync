package ru.i_novus.ms.rdm.sync.dao;

import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.ws.rs.core.MultivaluedMap;

public class LocalDataCriteria {
    private final String schemaTable;
    private final String pk;
    private final int limit;
    private final int offset;
    private final RdmSyncLocalRowState state;
    private final MultivaluedMap<String, Object> filters;
    private final DeletedCriteria deleted;

    public LocalDataCriteria(String schemaTable, String pk, int limit, int offset, RdmSyncLocalRowState state, MultivaluedMap<String, Object> filters, DeletedCriteria deleted) {
        this.schemaTable = schemaTable;
        this.pk = pk;
        this.limit = limit;
        this.offset = offset;
        this.state = state;
        this.filters = filters;
        this.deleted = deleted;
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

    public RdmSyncLocalRowState getState() {
        return state;
    }

    public MultivaluedMap<String, Object> getFilters() {
        return filters;
    }

    public DeletedCriteria getDeleted() {
        return deleted;
    }


}
