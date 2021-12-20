package ru.i_novus.ms.rdm.sync.dao.criteria;

import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.List;

public class LocalDataCriteria extends BaseDataCriteria {

    private final RdmSyncLocalRowState state;
    private final DeletedCriteria deleted;

    public LocalDataCriteria(String schemaTable, String pk, int limit, int offset,
                             List<FieldFilter> filters,
                             RdmSyncLocalRowState state,
                             DeletedCriteria deleted) {
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
