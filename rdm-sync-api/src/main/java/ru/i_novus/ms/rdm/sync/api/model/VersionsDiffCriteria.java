package ru.i_novus.ms.rdm.sync.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VersionsDiffCriteria extends RestCriteria {

    private final String refBookCode;

    private final String newVersion;

    private final String oldVersion;

    private final Set<String> fields;

    public VersionsDiffCriteria(String refBookCode, String newVersion, String oldVersion, Set<String> fields) {
        this.refBookCode = refBookCode;
        this.newVersion = newVersion;
        this.oldVersion = oldVersion;
        this.fields = fields;
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

    public Set<String> getFields() {
        return fields;
    }
}
