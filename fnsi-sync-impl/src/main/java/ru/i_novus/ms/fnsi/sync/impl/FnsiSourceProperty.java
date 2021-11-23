package ru.i_novus.ms.fnsi.sync.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "rdm-sync.source.fnsi")
public class FnsiSourceProperty {

    private List<FnsiSourcePropertyValue> values;

    public List<FnsiSourcePropertyValue> getValues() {
        return values;
    }

    public void setValues(List<FnsiSourcePropertyValue> values) {
        this.values = values;
    }

}
