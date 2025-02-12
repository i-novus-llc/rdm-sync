package ru.i_novus.ms.rdm.sync.init.loader;

import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.Objects;

public class AutoCreateRefBookPropertyValue {

    private String code;
    private String source;
    private String name;
    private SyncTypeEnum type;
    private String table;
    private String sysPkColumn = "_sync_rec_id";
    private String range;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public SyncTypeEnum getType() {
        return type;
    }

    public void setType(SyncTypeEnum type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getSysPkColumn() {
        return Objects.toString(sysPkColumn, "_sync_rec_id");
    }


    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }
}
