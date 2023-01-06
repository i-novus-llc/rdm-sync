package ru.i_novus.ms.rdm.sync.service.updater;

import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;

public interface RefBookUpdater {

    void update(RefBookVersion refBookVersion, DownloadResult downloadResult) throws RefBookUpdaterException;

}
