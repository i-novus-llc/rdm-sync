package ru.i_novus.ms.rdm.sync.dao;

import javax.ws.rs.core.MultivaluedMap;

public class VersionedLocalDataCriteria extends BaseDataCriteria {

    private final String version;

    public VersionedLocalDataCriteria(String schemaTable, String pk, int limit, int offset, MultivaluedMap<String, Object> filters, String version) {
        super(schemaTable, pk, limit, offset, filters);
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
