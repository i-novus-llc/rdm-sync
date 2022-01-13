package ru.i_novus.ms.rdm.sync.dao.criteria;

import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.List;

public class LocalDataCriteria extends BaseDataCriteria {

    private Long recordId = null;
    private RdmSyncLocalRowState state = RdmSyncLocalRowState.SYNCED;
    private DeletedCriteria deleted = null;
    private String sysPkColumn;

    public LocalDataCriteria(String schemaTable, String pk, int limit, int offset, List<FieldFilter> filters) {

        super(schemaTable, pk, limit, offset, filters);
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public RdmSyncLocalRowState getState() {
        return state;
    }

    public void setState(RdmSyncLocalRowState state) {
        this.state = state;
    }

    public DeletedCriteria getDeleted() {
        return deleted;
    }

    public void setDeleted(DeletedCriteria deleted) {
        this.deleted = deleted;
    }

    public String getSysPkColumn() {
        return sysPkColumn;
    }

    public void setSysPkColumn(String sysPkColumn) {
        this.sysPkColumn = sysPkColumn;
    }
}
