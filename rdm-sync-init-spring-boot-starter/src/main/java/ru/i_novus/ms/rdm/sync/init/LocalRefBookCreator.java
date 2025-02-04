package ru.i_novus.ms.rdm.sync.init;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;

/**
 * Автоматическое создание таблицы и маппинга для справочника
 */
public interface LocalRefBookCreator {

    void create(SyncMapping syncMapping, RefBookStructure structure);

}
