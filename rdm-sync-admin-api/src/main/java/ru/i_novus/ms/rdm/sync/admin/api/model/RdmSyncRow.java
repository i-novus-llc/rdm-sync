package ru.i_novus.ms.rdm.sync.admin.api.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Модель записи о синхронизации справочника.
 */
@Getter
@Setter
public class RdmSyncRow {

    /** Код справочника. */
    private String code;

    /** Наименование справочника. */
    private String name;

    /** Версия (номер) справочника. */
    private String version;

    /** Признак версионности. */
    private Boolean versioned;

    /** Признак автообновления. */
    private Boolean autoUpdatable;

    /** Источник для обновления. */
    private String source;

    /** Дата последнего обновления. */
    private LocalDateTime lastDateTime;

    /** Статус последнего обновления. */
    private String lastStatus;

    public RdmSyncRow() {
        // Nothing to do.
    }
}
