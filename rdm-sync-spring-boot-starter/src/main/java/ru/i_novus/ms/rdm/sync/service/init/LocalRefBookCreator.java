package ru.i_novus.ms.rdm.sync.service.init;

/**
 * Автоматическое создание таблицы и маппинга для справочника
 */
public interface LocalRefBookCreator {

    void create(String code, String source);
}
