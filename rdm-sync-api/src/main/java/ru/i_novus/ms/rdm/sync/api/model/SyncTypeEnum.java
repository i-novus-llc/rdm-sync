package ru.i_novus.ms.rdm.sync.api.model;

public enum SyncTypeEnum {

    /**
     * Синхронизируется только актуальные данные, неактульные помечаются как удаленные
     */
    ACTUAL_DATA,

    /**
     * Синхронизация с версиями
     */
    VERSIONED

}
