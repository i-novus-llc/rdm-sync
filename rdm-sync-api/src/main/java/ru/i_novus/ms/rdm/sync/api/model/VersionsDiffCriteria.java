package ru.i_novus.ms.rdm.sync.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VersionsDiffCriteria extends RestCriteria {

    private final String refBookCode;

    private final String newVersion;

    private final String oldVersion;

    private final RefBookStructure newVersionStructure;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    public VersionsDiffCriteria(String refBookCode, String newVersion, String oldVersion, RefBookStructure newVersionStructure) {
        this.refBookCode = refBookCode;
        this.newVersion = newVersion;
        this.oldVersion = oldVersion;
        this.newVersionStructure = newVersionStructure;
    }

    protected List<Sort.Order> getDefaultOrders() {
        return Collections.emptyList();
    }

    public List<Sort.Order> getOrders() {
        return this.getSort().get().collect(Collectors.toList());
    }

    public String getRefBookCode() {
        return refBookCode;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public RefBookStructure getNewVersionStructure() {
        return newVersionStructure;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }
}
