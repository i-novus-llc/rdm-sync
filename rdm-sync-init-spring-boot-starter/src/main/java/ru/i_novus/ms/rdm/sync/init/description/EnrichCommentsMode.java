package ru.i_novus.ms.rdm.sync.init.description;

/**
 * Режим запроса отсутствующих в маппинге комментариев для таблиц и колонок
 */
public enum EnrichCommentsMode {
    ALWAYS,
    ON_CREATE,
    NEVER
}
