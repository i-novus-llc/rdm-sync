package ru.i_novus.ms.rdm.sync.api.mapping;


import lombok.*;

import java.util.List;

@ToString
@AllArgsConstructor
@EqualsAndHashCode
public class SyncMapping {

    private VersionMapping versionMapping;

    private List<FieldMapping> fieldMapping;

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
