package ru.i_novus.ms.rdm.sync.service.downloader;

import javax.annotation.Nullable;

public interface RefBookDownloader {

    /**
     *
     * @param refCode - код справочника
     * @param version - версия справочника
     * @return Наименование таблицы куда скачались данные
     */
    String download(String refCode, @Nullable String version);
}
