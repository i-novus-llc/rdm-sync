package ru.i_novus.ms.rdm.sync.service.updater;

import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;

import java.time.LocalDateTime;

public interface RefBookUpdater {

    void update(RefBookVersion refBookVersion, DownloadResult downloadResult) throws RefBookUpdaterException;

    void afterSyncProcess(String refTable);

    void beforeSyncProcess(String refTable, LocalDateTime closedVersionPublishingDate, LocalDateTime newVersionPublishingDate);

}
