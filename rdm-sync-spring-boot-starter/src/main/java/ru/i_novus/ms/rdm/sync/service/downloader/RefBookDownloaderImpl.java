package ru.i_novus.ms.rdm.sync.service.downloader;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.persister.RetryingPageIterator;
import ru.i_novus.ms.rdm.sync.util.PageIterator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

public class RefBookDownloaderImpl implements RefBookDownloader {

    private final SyncSourceService syncSourceService;

    private final RdmSyncDao rdmSyncDao;

    private final int tries;

    private final int timeout;

    private final int maxSize;

    public RefBookDownloaderImpl(SyncSourceService syncSourceService, RdmSyncDao rdmSyncDao, int tries, int timeout, int maxSize) {
        this.syncSourceService = syncSourceService;
        this.rdmSyncDao = rdmSyncDao;
        this.tries = tries;
        this.timeout = timeout;
        this.maxSize = maxSize;
    }

    @Override
    public String download(String refCode, @Nullable String version) {
        String tableName = ("temp_" + refCode + "_" + version).replaceAll("\\.", "_");
        DataCriteria dataCriteria = new DataCriteria();
        dataCriteria.setVersion(version);
        dataCriteria.setCode(refCode);
        dataCriteria.setPageSize(maxSize);
        rdmSyncDao.createTempDataTbl(tableName);
        RetryingPageIterator<Map<String, Object>> iter = new RetryingPageIterator<>(new PageIterator<>
                (syncSourceService::getData, dataCriteria, true),
                tries, timeout);
        while (iter.hasNext()) {
            Page<? extends Map<String, ?>> page = iter.next();
            rdmSyncDao.insertTempData(tableName, new ArrayList(page.getContent()));
        }

        return tableName;
    }
}
