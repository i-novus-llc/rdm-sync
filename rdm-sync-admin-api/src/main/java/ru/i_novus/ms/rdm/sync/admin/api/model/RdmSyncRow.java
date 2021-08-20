package ru.i_novus.ms.rdm.sync.admin.api.model;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель записи о синхронизации справочника.
 */
@Getter
@Setter
public class RdmSyncRow implements Serializable {

    /** Код справочника. */
    private String code;

    /** Наименование справочника. */
    private String name;

    /** Версия (номер) справочника. */
    private String version;

    /** Признак версионности. */
    private boolean versioned;

    /** Признак автообновления. */
    private boolean autoUpdatable;

    /** Источник для обновления. */
    private String sourceType;

    /** Дата последнего обновления. */
    private LocalDateTime lastDateTime;

    /** Статус последнего обновления. */
    private String lastStatus;

    public RdmSyncRow() {
        // Nothing to do.
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
