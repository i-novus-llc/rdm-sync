package ru.i_novus.ms.rdm.sync.api.model;

import org.springframework.data.domain.Page;

public class VersionsDiff {

    private boolean structureChanged;

    private Page<RowDiff> rows;

    private VersionsDiff() {
        structureChanged = true;
    }

    private VersionsDiff(Page<RowDiff> rows) {
        this.rows = rows;
    }

    public boolean isStructureChanged() {
        return structureChanged;
    }

    public Page getRows() {
        return rows;
    }

    public static VersionsDiff structureChangedInstance() {
        return new VersionsDiff();
    }

    public static VersionsDiff dataChangedInstance(Page<RowDiff> rows) {
        return new VersionsDiff(rows);
    }
}
