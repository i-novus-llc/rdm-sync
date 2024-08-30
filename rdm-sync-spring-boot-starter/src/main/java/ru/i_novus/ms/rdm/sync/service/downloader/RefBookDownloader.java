package ru.i_novus.ms.rdm.sync.service.downloader;

import jakarta.annotation.Nullable;

public interface RefBookDownloader {

    /**
     *
     * @param refCode - код справочника
     * @param version - версия справочника
     * @return Наименование таблицы куда скачались данные и тип данных версия или разница между версиями
     */
    DownloadResult download(String refCode, @Nullable String version);
}
