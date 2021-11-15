package ru.i_novus.ms.rdm.sync.api.dao;

import java.util.Objects;

public class SyncSource {
    private final String name;
    private final String code;
    private final String initValues;
    private final String service;


    public SyncSource(String name, String code, String initValues, String service) {
        this.name = name;
        this.code = code;
        this.initValues = initValues;
        this.service = service;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getInitValues() {
        return initValues;
    }

    public String getService() {
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncSource that = (SyncSource) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(code, that.code) &&
                Objects.equals(initValues, that.initValues) &&
                Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, code, initValues, service);
    }

    @Override
    public String toString() {
        return "SyncSource{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", initValues='" + initValues + '\'' +
                ", service='" + service + '\'' +
                '}';
    }
}
