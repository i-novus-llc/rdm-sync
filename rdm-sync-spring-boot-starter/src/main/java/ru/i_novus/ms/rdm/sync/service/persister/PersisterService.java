package ru.i_novus.ms.rdm.sync.service.persister;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;

/**
 * Сервис реализующий логику записи данных из нси
 */
public interface PersisterService {
    /**
     * Первая запись данных справочника
     */
    void firstWrite(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult);

    void merge(RefBookVersion newVersion, String synchedVersion, VersionMapping versionMapping, DownloadResult downloadResult);

    void repeatVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult);

}
