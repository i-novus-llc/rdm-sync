package ru.i_novus.ms.rdm.sync.init.loader;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "rdm-sync.auto-create")
public class AutoCreateRefBookProperty {

    private List<AutoCreateRefBookPropertyValue> refbooks;

    public List<AutoCreateRefBookPropertyValue> getRefbooks() {
        return refbooks;
    }

    public void setRefbooks(List<AutoCreateRefBookPropertyValue> refbooks) {
        this.refbooks = refbooks;
    }
}
