package ru.i_novus.ms.rdm.sync.api.mapping;


import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class VersionAndFieldMapping {

    private VersionMapping versionMapping;

    private List<FieldMapping> fieldMapping;

    public int getMappingVersion() {
        return versionMapping.getMappingVersion();
    }

    public VersionMapping getVersionMapping() {
        return versionMapping;
    }

    public void setVersionMapping(VersionMapping versionMapping) {
        this.versionMapping = versionMapping;
    }

    public List<FieldMapping> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(List<FieldMapping> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

}
