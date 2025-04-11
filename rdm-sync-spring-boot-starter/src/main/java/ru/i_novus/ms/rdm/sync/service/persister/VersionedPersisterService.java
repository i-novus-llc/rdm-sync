package ru.i_novus.ms.rdm.sync.service.persister;

import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDao;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResultType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        List<String> fields = rdmSyncDao.getFieldMappings(versionMapping.getId()).stream().map(FieldMapping::getSysField).collect(Collectors.toList());
        versionedDataDao.addFirstVersionData(downloadResult.getTableName(), versionMapping.getTable(), versionMapping.getPrimaryField(), newVersion.getFrom(), newVersion.getTo(), fields);
    }

    @Override
    public void merge(RefBookVersion newVersion, String syncedVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        if (DownloadResultType.VERSION.equals(downloadResult.getType())) {
            firstWrite(newVersion, versionMapping, downloadResult);
        } else {
            List<String> fields = rdmSyncDao.getFieldMappings(versionMapping.getId()).stream().map(FieldMapping::getSysField).collect(Collectors.toList());
            versionedDataDao.addDiffVersionData(downloadResult.getTableName(), versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getCode(), newVersion.getFrom(), newVersion.getTo(), fields, syncedVersion);
        }
        versionedDataDao.mergeIntervals(versionMapping.getTable());
    }

    @Override
    public void repeatVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        List<String> fields = rdmSyncDao.getFieldMappings(versionMapping.getId()).stream().map(FieldMapping::getSysField).collect(Collectors.toList());
        versionedDataDao.repeatVersion(downloadResult.getTableName(), versionMapping.getTable(), versionMapping.getPrimaryField(), newVersion.getFrom(), newVersion.getTo(), fields);
        versionedDataDao.mergeIntervals(versionMapping.getTable());
    }

    @Override
    public void beforeSyncProcess(String refTable, LocalDateTime closedVersionPublishingDate, LocalDateTime newVersionPublishingDate) {
        versionedDataDao.closeIntervals(refTable, closedVersionPublishingDate, newVersionPublishingDate);
    }
}
