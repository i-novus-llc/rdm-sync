package ru.i_novus.ms.rdm.sync.api.model;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class DataCriteria extends RestCriteria {

    private String code;
    private String version;

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
}
