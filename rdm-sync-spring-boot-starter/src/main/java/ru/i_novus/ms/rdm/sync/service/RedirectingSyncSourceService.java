package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.VersionsDiff;
import ru.i_novus.ms.rdm.sync.api.model.VersionsDiffCriteria;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Map;
import java.util.Set;

@Component
public class RedirectingSyncSourceService implements SyncSourceService {

    private final SyncSourceDao syncSourceDao;

    private final RdmSyncDao syncDao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactorySet;

    @Autowired
    public RedirectingSyncSourceService(SyncSourceDao syncSourceDao, RdmSyncDao syncDao, Set<SyncSourceServiceFactory> syncSourceServiceFactorySet) {
        this.syncSourceDao = syncSourceDao;
        this.syncDao = syncDao;
        this.syncSourceServiceFactorySet = syncSourceServiceFactorySet;
    }

    @Override
    public RefBook getRefBook(String code) {
        return getSyncSourceService(code).getRefBook(code);
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        return getSyncSourceService(dataCriteria.getCode()).getData(dataCriteria);
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {
        return getSyncSourceService(criteria.getRefBookCode()).getDiff(criteria);
    }


    private SyncSourceService getSyncSourceService(String refBookCode) {
        SyncSource source = getSource(refBookCode);
        return syncSourceServiceFactorySet.stream().filter(factory ->
                factory.isSatisfied(source)).findAny().orElse(null).createService(source);
    }

    private SyncSource getSource(String refBookCode) {
        VersionMapping versionMapping = syncDao.getVersionMapping(refBookCode, "CURRENT");
        return syncSourceDao.findByCode(versionMapping.getSource());
    }
}
