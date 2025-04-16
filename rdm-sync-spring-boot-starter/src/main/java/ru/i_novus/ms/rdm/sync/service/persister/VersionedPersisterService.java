package ru.i_novus.ms.rdm.sync.service.persister;

import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDao;
import ru.i_novus.ms.rdm.sync.init.dao.pg.impl.PgTable;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResultType;

@Service
public class VersionedPersisterService implements PersisterService {

    private final RdmSyncDao rdmSyncDao;

    private final VersionedDataDao versionedDataDao;

    public VersionedPersisterService(RdmSyncDao rdmSyncDao, VersionedDataDao versionedDataDao) {
        this.rdmSyncDao = rdmSyncDao;
        this.versionedDataDao = versionedDataDao;
    }

    @Override
    public void firstWrite(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        PgTable pgTable = new PgTable(versionMapping, rdmSyncDao.getFieldMappings(versionMapping.getId()));
        versionedDataDao.addFirstVersionData(downloadResult.getTableName(), pgTable, newVersion.getVersionId());
    }

    @Override
    public void merge(RefBookVersion newVersion, String syncedVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        if (DownloadResultType.VERSION.equals(downloadResult.getType())) {
            firstWrite(newVersion, versionMapping, downloadResult);
        } else {
            PgTable pgTable = new PgTable(versionMapping, rdmSyncDao.getFieldMappings(versionMapping.getId()));
            versionedDataDao.addDiffVersionData(downloadResult.getTableName(), pgTable, versionMapping.getCode(), newVersion.getVersionId(), syncedVersion);
        }
    }

    @Override
    public void repeatVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        PgTable pgTable = new PgTable(versionMapping, rdmSyncDao.getFieldMappings(versionMapping.getId()));
        LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion());
        versionedDataDao.repeatVersion(downloadResult.getTableName(), pgTable, loadedVersion.getId());
    }
}
