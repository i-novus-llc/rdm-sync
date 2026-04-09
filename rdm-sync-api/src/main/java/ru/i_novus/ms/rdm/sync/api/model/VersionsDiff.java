package ru.i_novus.ms.rdm.sync.api.model;

import org.springframework.data.domain.Page;

public class VersionsDiff {

    private boolean structureChanged;

    private boolean incompleteData;

    private Page<RowDiff> rows;

    private VersionsDiff() {
        structureChanged = true;
    }

    private VersionsDiff(boolean incompleteData) {
        this.incompleteData = incompleteData;
    }

    private VersionsDiff(Page<RowDiff> rows) {
        this.rows = rows;
    }

    public boolean isStructureChanged() {
        return structureChanged;
    }

    /**
     * Diff нельзя применить — нужна полная загрузка версии.
     * Например, изменилась структура или в diff пришли неполные данные (отсутствует первичный ключ).
     */
    public boolean isDiffInapplicable() {
        return structureChanged || incompleteData;
    }

    public Page<RowDiff> getRows() {
        return rows;
    }

    public static VersionsDiff structureChangedInstance() {
        return new VersionsDiff();
    }

    public static VersionsDiff incompleteDataInstance() {
        return new VersionsDiff(true);
    }

    public static VersionsDiff dataChangedInstance(Page<RowDiff> rows) {
        return new VersionsDiff(rows);
    }
}
