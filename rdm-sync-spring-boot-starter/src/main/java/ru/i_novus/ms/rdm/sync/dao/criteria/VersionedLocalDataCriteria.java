package ru.i_novus.ms.rdm.sync.dao.criteria;

import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;

import java.util.List;

public class VersionedLocalDataCriteria extends BaseDataCriteria {

    private String refBookCode;

    private String version;

    private Boolean actual;

    public VersionedLocalDataCriteria(String refBookCode,
                                      String schemaTable, String pk, int limit, int offset,
                                      List<FieldFilter> filters,
                                      String version) {
        super(schemaTable, pk, limit, offset, filters);
        this.version = version;
        this.refBookCode = refBookCode;
    }

    public String getRefBookCode() {
        return refBookCode;
    }

    public void setRefBookCode(String refBookCode) {
        this.refBookCode = refBookCode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getActual() {
        return actual;
    }

    public void setActual(Boolean actual) {
        this.actual = actual;
    }
}
