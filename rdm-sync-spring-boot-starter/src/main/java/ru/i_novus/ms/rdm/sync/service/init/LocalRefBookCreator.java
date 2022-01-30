package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import javax.annotation.Nullable;

/**
 * Автоматическое создание таблицы и маппинга для справочника
 */
public interface LocalRefBookCreator {

    void create(String refCode, String refName, String source, SyncTypeEnum type, String table, String sysPkColumn, @Nullable String range);
}
