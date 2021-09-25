package ru.i_novus.ms.rdm.sync.api.model;

import java.util.Map;

public class RowDiff {

    private final RowDiffStatus status;

    private final Map<String, Object> row;

    public RowDiff(RowDiffStatus status, Map<String, Object> row) {
        this.status = status;
        this.row = row;
    }

    public RowDiffStatus getStatus() {
        return status;
    }

    public Map<String, Object> getRow() {
        return row;
    }
}
