package ru.i_novus.ms.rdm.sync.api.model;

import java.time.LocalDateTime;

public class RefBook {

    private String code;

    private String lastVersion;

    private LocalDateTime lastPublishDate;

    private Integer lastVersionId;

    private RefBookStructure structure;

    public RefBook() {
    }

    public RefBook(RefBook source) {
        this.code = source.getCode();
        this.lastVersion = source.getLastVersion();
        this.lastPublishDate = source.getLastPublishDate();
        this.lastVersionId = source.getLastVersionId();
        this.structure = source.getStructure();
    }

    public Integer getLastVersionId() {
        return lastVersionId;
    }

    public void setLastVersionId(Integer lastVersionId) {
        this.lastVersionId = lastVersionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLastVersion() {
        return lastVersion;
    }

    public void setLastVersion(String lastVersion) {
        this.lastVersion = lastVersion;
    }

    public LocalDateTime getLastPublishDate() {
        return lastPublishDate;
    }

    public void setLastPublishDate(LocalDateTime lastPublishDate) {
        this.lastPublishDate = lastPublishDate;
    }

    public RefBookStructure getStructure() {
        return structure;
    }

    public void setStructure(RefBookStructure structure) {
        this.structure = structure;
    }
}
