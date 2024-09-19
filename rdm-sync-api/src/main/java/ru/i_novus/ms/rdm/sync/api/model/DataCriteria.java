package ru.i_novus.ms.rdm.sync.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DataCriteria extends RestCriteria {

    private String code;
    private String version;

    /**
     * Кэширование id версии для исключения лишних запросов id.
     */
    private Integer versionId;

    /**
     * Множество полей, по которым нужны значения.
     */
    private Set<String> fields;

    /**
     * Кэширование структуры версии для исключения лишних запросов структуры.
     */
    private RefBookStructure refBookStructure;

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return new ArrayList<>();
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

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public RefBookStructure getRefBookStructure() {
        return refBookStructure;
    }

    public void setRefBookStructure(RefBookStructure refBookStructure) {
        this.refBookStructure = refBookStructure;
    }

    public void setAbsentVersionId(Integer versionId) {
        if (this.versionId == null) {
            this.versionId = versionId;
        }
    }
}
