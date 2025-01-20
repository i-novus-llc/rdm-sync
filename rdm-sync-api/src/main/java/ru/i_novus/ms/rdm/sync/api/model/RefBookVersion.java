package ru.i_novus.ms.rdm.sync.api.model;

import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
public class RefBookVersion extends RefBookVersionItem {

    private RefBookStructure structure;

    public RefBookVersion() {
    }

    public RefBookVersion(RefBookVersionItem refBookVersionItem,  RefBookStructure structure) {
        super(refBookVersionItem.getCode(), refBookVersionItem.getVersion(), refBookVersionItem.getFrom(), refBookVersionItem.getTo(), refBookVersionItem.getVersionId());
        this.structure = structure;
    }

    public RefBookVersion(String code, String version, LocalDateTime from, LocalDateTime to, Integer versionId, RefBookStructure structure) {
        super(code, version, from, to, versionId);
        this.structure = structure;
    }

    public RefBookStructure getStructure() {
        return structure;
    }

    public void setStructure(RefBookStructure structure) {
        this.structure = structure;
    }
}
