package ru.i_novus.ms.rdm.sync.service.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

@Transactional
public class RdmNotVersionedRefBookUpdater extends BaseRefBookUpdater {

    private static final Logger logger = LoggerFactory.getLogger(RdmNotVersionedRefBookUpdater.class);

    private final PersisterService persisterService;


    public RdmNotVersionedRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, PersisterService persisterService, RdmLoggingService loggingService) {
        super(dao, syncSourceService, loggingService);
        this.persisterService = persisterService;
    }

    @Override
    protected PersisterService getPersisterService() {
        return persisterService;
    }

    /*    @Override
    protected void updateProcessing(RefBookVersion newVersion, VersionMapping versionMapping) {
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion());
            if (loadedVersion == null) {
                //заливаем с нуля
                persisterService.firstWrite(newVersion, versionMapping, syncSourceService);

            } else if (isNewVersion(newVersion, loadedVersion)) {
                //если дата публикация не совпадают - нужно обновить справочник
                persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);

            } else if (isMappingChanged(versionMapping, loadedVersion)) {
                //Значит в прошлый раз мы синхронизировались по старому маппингу.
                //Необходимо полностью залить свежую версию.
                persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);
            }
            if (loadedVersion != null) {
                //обновляем версию в таблице версий клиента
                dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getVersion(), newVersion.getFrom(), null);
            } else {
                dao.insertLoadedVersion(newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo(), true);
            }
            logger.info("{} sync finished", newVersion.getCode());
    }*/

}
