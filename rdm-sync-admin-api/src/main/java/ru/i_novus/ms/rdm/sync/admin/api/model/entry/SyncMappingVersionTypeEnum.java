package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

/**
 * Тип указания версий, к которым применим маппинг.
 */
public enum SyncMappingVersionTypeEnum {

    UNIQUE, // одна версия
    LIST,   // список версий
    RANGE   // диапазон версий (начальная и конечная версии)
    ;
}
