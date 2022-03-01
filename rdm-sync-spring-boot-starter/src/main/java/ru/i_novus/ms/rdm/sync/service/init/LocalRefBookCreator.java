package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;

/**
 * Автоматическое создание таблицы и маппинга для справочника
 */
public interface LocalRefBookCreator {

    void create(VersionAndFieldMapping versionAndFieldMapping);

}
