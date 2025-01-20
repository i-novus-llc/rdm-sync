package ru.i_novus.ms.rdm.sync.service.downloader;

import jakarta.annotation.Nullable;

public interface RefBookDownloader {

    /**
     * Загрузка указанной версии справочника.
     *
     * @param refCode - код справочника
     * @param version - версия справочника
     * @return Наименование таблицы, куда скачались записи версии или записи разницы между версиями
     */
    DownloadResult download(String refCode, @Nullable String version);
}
