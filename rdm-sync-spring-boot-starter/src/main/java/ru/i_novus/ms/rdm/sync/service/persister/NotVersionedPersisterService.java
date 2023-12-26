package ru.i_novus.ms.rdm.sync.service.persister;

import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResultType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Хранит данные как актуальные и неактуальные, версионность отсутствует
 */
@Service
public class NotVersionedPersisterService implements PersisterService {

    private final RdmSyncDao dao;

    public NotVersionedPersisterService(RdmSyncDao dao) {
        this.dao = dao;
    }

    @Override
    public void firstWrite(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getId());
        insertOrUpdateRows(downloadResult.getTableName(), versionMapping, fieldMappings, newVersion.getFrom());
    }

    @Override
    public void merge(RefBookVersion newVersion, String syncedVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        List<String> fields = dao.getFieldMappings(versionMapping.getId()).stream().map(FieldMapping::getSysField).collect(Collectors.toList());

        if (DownloadResultType.VERSION.equals(downloadResult.getType())) {
            firstWrite(newVersion, versionMapping, downloadResult);
            return;
        }

        if (DownloadResultType.DIFF.equals(downloadResult.getType())) {
            dao.migrateDiffTempData(downloadResult.getTableName(), versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), fields, newVersion.getFrom());
        }
    }

    @Override
    public void repeatVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        firstWrite(newVersion, versionMapping, downloadResult);
    }


    private void insertOrUpdateRows(String tempDataTbl, VersionMapping versionMapping, List<FieldMapping> fieldMappings, LocalDateTime deletedTs) {
        List<String> fields = fieldMappings.stream().map(FieldMapping::getSysField).collect(Collectors.toList());
        dao.migrateNotVersionedTempData(tempDataTbl, versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), fields, deletedTs);
    }

}
