package ru.i_novus.ms.rdm.sync.api.model;

public enum SyncTypeEnum {

    /**
     * Синхронизируется только актуальные данные, неактуальные помечаются как удаленные
     */
    NOT_VERSIONED,

    /**
     * Синхронизация с паспортом и версией
     */
    SIMPLE_VERSIONED,

    /**
     * Синхронизация неверсионируемых справочников
     */
    RDM_NOT_VERSIONED

}
