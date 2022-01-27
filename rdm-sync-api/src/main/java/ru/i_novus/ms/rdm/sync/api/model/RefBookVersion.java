package ru.i_novus.ms.rdm.sync.api.model;

import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode
public class RefBookVersion {

    private String code;

    private String version;

    private LocalDateTime from;

    private LocalDateTime to;

    private Integer versionId;

    private RefBookStructure structure;

    public RefBookVersion() {
    }

    public RefBookVersion(String code, String version, LocalDateTime from, LocalDateTime to, Integer versionId, RefBookStructure structure) {
        this.code = code;
        this.version = version;
        this.from = from;
        this.to = to;
        this.versionId = versionId;
        this.structure = structure;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public RefBookStructure getStructure() {
        return structure;
    }

    public void setStructure(RefBookStructure structure) {
        this.structure = structure;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }
}
