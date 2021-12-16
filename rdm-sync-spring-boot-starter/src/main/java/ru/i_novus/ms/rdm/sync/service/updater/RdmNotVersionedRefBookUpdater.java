package ru.i_novus.ms.rdm.sync.service.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterServiceLocator;

@Component
@Transactional
public class RdmNotVersionedRefBookUpdater extends BaseRefBookUpdater {

    private static final Logger logger = LoggerFactory.getLogger(RdmNotVersionedRefBookUpdater.class);

    private final RdmSyncDao dao;

    private final SyncSourceService syncSourceService;

    private final PersisterServiceLocator persisterServiceLocator;

    private final RdmLoggingService loggingService;

    public RdmNotVersionedRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, PersisterServiceLocator persisterServiceLocator, RdmLoggingService loggingService) {
        super(dao, syncSourceService, loggingService);
        this.dao = dao;
        this.syncSourceService = syncSourceService;
        this.persisterServiceLocator = persisterServiceLocator;
        this.loggingService = loggingService;
    }

    @Override
    protected boolean isNewVersionPublished(RefBook newVersion, LoadedVersion loadedVersion) {
        return loadedVersion.getPublicationDate().isBefore(newVersion.getLastPublishDate());
    }

    @Override
    protected void updateProcessing(RefBook newVersion, VersionMapping versionMapping) {
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode());
            PersisterService persisterService = persisterServiceLocator.getPersisterService(versionMapping.getCode());
            if (loadedVersion == null) {
                //заливаем с нуля
                persisterService.firstWrite(newVersion, versionMapping, syncSourceService);

            } else if (isNewVersionPublished(newVersion, loadedVersion)) {
                //если дата публикация не совпадают - нужно обновить справочник
                persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);

            } else if (isMappingChanged(versionMapping, loadedVersion)) {
                //Значит в прошлый раз мы синхронизировались по старому маппингу.
                //Необходимо полностью залить свежую версию.
                persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);
            }
            if (loadedVersion != null) {
                //обновляем версию в таблице версий клиента
                dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
            } else {
                dao.insertLoadedVersion(newVersion.getCode(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
            }
            logger.info("{} sync finished", newVersion.getCode());
    }

}
