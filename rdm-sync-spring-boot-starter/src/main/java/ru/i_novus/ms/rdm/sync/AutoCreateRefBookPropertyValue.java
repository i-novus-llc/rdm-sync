package ru.i_novus.ms.rdm.sync;

import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

public class AutoCreateRefBookPropertyValue {

    private String code;
    private String source;
    private String name;
    private SyncTypeEnum type;
    private String table;
    private String sysPkColumn = "_sync_rec_id";

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
        return sysPkColumn;
    }

    public void setSysPkColumn(String sysPkColumn) {
        this.sysPkColumn = sysPkColumn;
    }
}
