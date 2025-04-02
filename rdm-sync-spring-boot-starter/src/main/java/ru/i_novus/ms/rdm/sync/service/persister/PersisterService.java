package ru.i_novus.ms.rdm.sync.service.persister;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;

import java.time.LocalDateTime;

/**
 * Сервис реализующий логику записи данных из нси
 */
public interface PersisterService {
    /**
     * Первая запись данных справочника
     */
    void firstWrite(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult);

    void merge(RefBookVersion newVersion, String syncedVersion, VersionMapping versionMapping, DownloadResult downloadResult);

    void repeatVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult);

    void afterSyncProcess(String refTable);

    void beforeSyncProcess(String refTable, LocalDateTime closedVersionPublishingDate, LocalDateTime newVersionPublishingDate);
}
