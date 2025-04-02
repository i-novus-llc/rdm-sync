package ru.i_novus.ms.rdm.sync.service.persister;

import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SimpleVersionedPersisterService implements PersisterService {

    private final RdmSyncDao rdmSyncDao;

    public SimpleVersionedPersisterService(RdmSyncDao rdmSyncDao) {
        this.rdmSyncDao = rdmSyncDao;
    }

    @Override
    public void firstWrite(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        List<String> fields = rdmSyncDao.getFieldMappings(versionMapping.getId()).stream().map(FieldMapping::getSysField).collect(Collectors.toList());
        LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion());
        rdmSyncDao.migrateSimpleVersionedTempData(downloadResult.getTableName(), versionMapping.getTable(), versionMapping.getPrimaryField(), loadedVersion.getId(), fields);
    }

    @Override
    public void merge(RefBookVersion newVersion, String syncedVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        firstWrite(newVersion, versionMapping, downloadResult);
    }

    @Override
    public void repeatVersion(RefBookVersion refBookVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        List<String> fields = rdmSyncDao.getFieldMappings(versionMapping.getId()).stream().map(FieldMapping::getSysField).collect(Collectors.toList());
        LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(refBookVersion.getCode(), refBookVersion.getVersion());
        rdmSyncDao.reMigrateSimpleVersionedTempData(downloadResult.getTableName(), versionMapping.getTable(), versionMapping.getPrimaryField(), loadedVersion.getId(), fields);
    }

    @Override
    public void afterSyncProcess(String refTable) {
        //nothing
    }

    @Override
    public void beforeSyncProcess(String refTable, LocalDateTime closedVersionPublishingDate, LocalDateTime newVersionPublishingDate) {
        //nothing
    }
}
