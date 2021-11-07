package ru.i_novus.ms.rdm.sync.dao;

import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.ws.rs.core.MultivaluedMap;

public class LocalDataCriteria extends BaseDataCriteria {

    private final RdmSyncLocalRowState state;
    private final DeletedCriteria deleted;

    public LocalDataCriteria(String schemaTable, String pk, int limit, int offset, RdmSyncLocalRowState state, MultivaluedMap<String, Object> filters, DeletedCriteria deleted) {
        super(schemaTable, pk, limit, offset, filters);
        this.state = state;
        this.deleted = deleted;
    }

    public RdmSyncLocalRowState getState() {
        return state;
    }

    public DeletedCriteria getDeleted() {
        return deleted;
    }

}
