package ru.i_novus.ms.rdm.sync.service.persister;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;

/**
 * Сервис реализующий логику записи данных из нси
 */
public interface PersisterService {
    /**
     * Первая запись данных справочника
     */
    void firstWrite(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService);

    void merge(RefBook newVersion, String synchedVersion, VersionMapping versionMapping, SyncSourceService syncSourceService);

    void repeatVersion(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService);

}
