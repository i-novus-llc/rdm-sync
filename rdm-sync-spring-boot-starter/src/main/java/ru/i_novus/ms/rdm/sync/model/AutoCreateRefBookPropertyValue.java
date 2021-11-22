package ru.i_novus.ms.rdm.sync.model;

import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

public class AutoCreateRefBookPropertyValue {

    private String name;
    private String code;
    private SyncTypeEnum type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public SyncTypeEnum getType() {
        return type;
    }

    public void setType(SyncTypeEnum type) {
        this.type = type;
    }
}
