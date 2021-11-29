package ru.i_novus.ms.rdm.sync.service.updater;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterServiceLocator;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Component
public class NotVersionedRefBookUpdater implements RefBookUpdater {

    private static final Logger logger = LoggerFactory.getLogger(NotVersionedRefBookUpdater.class);

    private final RdmSyncDao dao;

    private final SyncSourceService syncSourceService;

    private final PersisterServiceLocator persisterServiceLocator;

    private final RdmLoggingService loggingService;

    public NotVersionedRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, PersisterServiceLocator persisterServiceLocator, RdmLoggingService loggingService) {
        this.dao = dao;
        this.syncSourceService = syncSourceService;
        this.persisterServiceLocator = persisterServiceLocator;
        this.loggingService = loggingService;
    }

    @Override
    public void update(String refCode) {
        if (dao.getVersionMapping(refCode, "CURRENT") == null) {
            logger.error("No version mapping found for reference book with code '{}'.", refCode);
            return;
        }

        RefBook newVersion;
        try {
            newVersion = getLastPublishedVersion(refCode);

        } catch (Exception e) {
            logger.error(String.format("Error while fetching new version with code '%s'.", refCode), e);
            return;
        }

        VersionMapping versionMapping = getVersionMapping(refCode);
        LoadedVersion loadedVersion = dao.getLoadedVersion(refCode);
        try {
            if (loadedVersion == null || isNewVersionPublished(newVersion, loadedVersion) || isMappingChanged(versionMapping, loadedVersion)) {

                update(newVersion, versionMapping);
                loggingService.logOk(refCode, versionMapping.getVersion(), newVersion.getLastVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refCode);
            }
        } catch (Exception e) {
            logger.error(String.format("Error while updating new version with code '%s'.", refCode), e);

            loggingService.logError(refCode, versionMapping.getVersion(), newVersion.getLastVersion(),
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
        }

    }

    private RefBook getLastPublishedVersion(String refBookCode) {
        RefBook refBook = syncSourceService.getRefBook(refBookCode);
        if (refBook == null)
            throw new IllegalArgumentException(String.format("Reference book with code '%s' not found.", refBookCode));

        if (!refBook.getStructure().hasPrimary())
            throw new IllegalStateException(String.format("Reference book with code '%s' has not primary key.", refBookCode));
        return refBook;
    }

    private VersionMapping getVersionMapping(String refBookCode) {

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode, "CURRENT");
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());

        final String primaryField = versionMapping.getPrimaryField();
        if (fieldMappings.stream().noneMatch(mapping -> mapping.getSysField().equals(primaryField)))
            throw new IllegalArgumentException(String.format("No mapping found for primary key '%s'.", primaryField));

        return versionMapping;
    }

    private boolean isNewVersionPublished(RefBook newVersion, LoadedVersion loadedVersion) {

        return !loadedVersion.getVersion().equals(newVersion.getLastVersion())
                && !loadedVersion.getPublicationDate().equals(newVersion.getLastPublishDate());
    }

    private boolean isMappingChanged(VersionMapping versionMapping, LoadedVersion loadedVersion) {
        return versionMapping.getMappingLastUpdated().isAfter(loadedVersion.getLastSync());
    }

    private void update(RefBook newVersion, VersionMapping versionMapping) {

        logger.info("{} sync started", newVersion.getCode());
        // Если изменилась структура, проверяем актуальность полей в маппинге
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());
        validateStructureAndMapping(newVersion, fieldMappings);
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode());
        if (dao.existsInternalLocalRowStateUpdateTrigger(versionMapping.getTable())){
            dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }
        try {
            PersisterService persisterService = persisterServiceLocator.getPersisterService(versionMapping.getCode());
            if (loadedVersion == null) {
                //заливаем с нуля
                persisterService.firstWrite(newVersion, versionMapping, syncSourceService);

            } else if (isNewVersionPublished(newVersion, loadedVersion)) {
                //если версия и дата публикация не совпадают - нужно обновить справочник
                persisterService.merge(newVersion, loadedVersion.getVersion(), versionMapping, syncSourceService);

            } else if (isMappingChanged(versionMapping, loadedVersion)) {
//              Значит в прошлый раз мы синхронизировались по старому маппингу.
//              Необходимо полностью залить свежую версию.
                persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);
            }
            if (loadedVersion != null) {
                //обновляем версию в таблице версий клиента
                dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
            } else {
                dao.insertLoadedVersion(newVersion.getCode(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
            }
            logger.info("{} sync finished", newVersion.getCode());
        } catch (Exception e) {
            logger.error("cannot sync " + versionMapping.getCode(), e);
        } finally {
            if (dao.existsInternalLocalRowStateUpdateTrigger(versionMapping.getTable())){
                dao.enableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
            }
        }
    }

    private void validateStructureAndMapping(RefBook newVersion, List<FieldMapping> fieldMappings) {

        List<String> clientRdmFields = fieldMappings.stream().map(FieldMapping::getRdmField).collect(toList());
        Set<String> actualFields = newVersion.getStructure().getAttributesAndTypes().keySet();
        if (!actualFields.containsAll(clientRdmFields)) {
            // В новой версии удалены поля, которые ведутся в системе
            clientRdmFields.removeAll(actualFields);
            throw new IllegalStateException(String.format("Field '%s' was deleted in version with code '%s'. Update your mappings.",
                    String.join(",", clientRdmFields), newVersion.getCode()));
        }

    }

}
