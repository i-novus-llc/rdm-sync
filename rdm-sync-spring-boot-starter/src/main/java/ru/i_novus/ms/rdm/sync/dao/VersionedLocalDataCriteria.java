package ru.i_novus.ms.rdm.sync.dao;

import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;

import java.util.List;

public class VersionedLocalDataCriteria extends BaseDataCriteria {

    private final String version;

    public VersionedLocalDataCriteria(String schemaTable, String pk, int limit, int offset,
                                      List<FieldFilter> filters,
                                      String version) {
        super(schemaTable, pk, limit, offset, filters);

        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
