package ru.i_novus.ms.rdm.sync.service.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public abstract class BaseRefBookUpdater implements RefBookUpdater {

    private static final Logger logger = LoggerFactory.getLogger(BaseRefBookUpdater.class);

    protected final RdmSyncDao dao;

    protected final SyncSourceService syncSourceService;

    private final RdmLoggingService loggingService;

    protected abstract PersisterService getPersisterService();

    protected BaseRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, RdmLoggingService loggingService) {
        this.dao = dao;
        this.syncSourceService = syncSourceService;
        this.loggingService = loggingService;
    }

    @Override
    public void update(String refCode, String version) throws RefBookUpdaterException {
        logger.info("try to load {} version: {}", refCode, version);
        RefBookVersion newVersion;
        try {
            newVersion = getRefBookVersion(refCode, version);

        } catch (Exception e) {
            logger.error(String.format("Error while fetching new version with code '%s'.", refCode), e);
            return;
        }
        VersionMapping versionMapping = getVersionMapping(newVersion);
        if (versionMapping == null) {
            logger.error("No version mapping found for reference book with code '{}'.", refCode);
            return;
        }

        LoadedVersion loadedVersion = dao.getLoadedVersion(refCode, newVersion.getVersion());
        try {
            if (!dao.existsLoadedVersion(refCode) || loadedVersion == null || isMappingChanged(versionMapping, loadedVersion)
                    || (isNewVersionPublished(newVersion, loadedVersion)) && versionMapping.getType().equals(SyncTypeEnum.RDM_NOT_VERSIONED)) {

                update(newVersion, versionMapping);
                loggingService.logOk(refCode, versionMapping.getRefBookVersion(), newVersion.getVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refCode);
            }
        } catch (final Exception e) {
            throw new RefBookUpdaterException(
                e,
                versionMapping,
                newVersion
            );
        }
    }

    private RefBookVersion getRefBookVersion(String refBookCode, String version) {
        RefBookVersion refBook = syncSourceService.getRefBook(refBookCode, version);
        if (refBook == null)
            throw new IllegalArgumentException(String.format("Reference book with code '%s' not found.", refBookCode));

        if (!refBook.getStructure().hasPrimary())
            throw new IllegalStateException(String.format("Reference book with code '%s' has not primary key.", refBookCode));
        return refBook;
    }

    private VersionMapping getVersionMapping(RefBookVersion refBookVersion) {
        VersionMapping versionMapping = dao.getVersionMapping(refBookVersion.getCode(), refBookVersion.getVersion());
        if (versionMapping == null) {
            versionMapping = dao.getVersionMapping(refBookVersion.getCode(), "CURRENT");
        }
        if (versionMapping == null) {
            return null;
        }
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getId());

        final String primaryField = versionMapping.getPrimaryField();
        if (fieldMappings.stream().noneMatch(mapping -> mapping.getSysField().equals(primaryField)))
            throw new IllegalArgumentException(String.format("No mapping found for primary key '%s'.", primaryField));

        return versionMapping;
    }

    protected boolean isMappingChanged(VersionMapping versionMapping, LoadedVersion loadedVersion) {
        return versionMapping.getMappingLastUpdated().isAfter(loadedVersion.getLastSync());
    }

    protected void update(RefBookVersion newVersion, VersionMapping versionMapping) {
        logger.info("{} sync started", newVersion.getCode());
        // Если изменилась структура, проверяем актуальность полей в маппинге
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getId());
        validateStructureAndMapping(newVersion, fieldMappings);
        boolean haveTrigger = dao.existsInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        if (haveTrigger) {
            dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }

        try {
            updateProcessing(newVersion, versionMapping);
        } finally {
            if (haveTrigger) {
                dao.enableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
            }
        }
    }

    private void validateStructureAndMapping(RefBookVersion newVersion, List<FieldMapping> fieldMappings) {
        List<String> clientRdmFields = fieldMappings.stream().map(FieldMapping::getRdmField).collect(toList());
        Set<String> actualFields = newVersion.getStructure().getAttributesAndTypes().keySet();
        if (!actualFields.containsAll(clientRdmFields)) {
            // В новой версии удалены поля, которые ведутся в системе
            clientRdmFields.removeAll(actualFields);
            throw new IllegalStateException(String.format("Field '%s' was deleted in version with code '%s'. Update your mappings.",
                    String.join(",", clientRdmFields), newVersion.getCode()));
        }

    }

    private boolean isNewVersionPublished(RefBookVersionItem newVersion, LoadedVersion loadedVersion) {
        return loadedVersion.getPublicationDate().isBefore(newVersion.getFrom());
    }

    protected void updateProcessing(RefBookVersion newVersion, VersionMapping versionMapping) {
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion());
        if (loadedVersion == null && !dao.existsLoadedVersion(newVersion.getCode())) {
            addFirstVersion(newVersion, versionMapping);

        } else if (loadedVersion == null) {
            addNewVersion(newVersion, versionMapping);

        } else if (isMappingChanged(versionMapping, loadedVersion) || newVersion.getFrom().isAfter(loadedVersion.getPublicationDate())) {
            //Значит в прошлый раз мы синхронизировались по старому маппингу.
            //Необходимо полностью залить свежую версию.
            editVersion(newVersion, versionMapping, loadedVersion);
        }

        logger.info("{}, version {} sync finished", newVersion.getCode(), newVersion.getVersion());
    }

    protected void editVersion(RefBookVersion newVersion, VersionMapping versionMapping, LoadedVersion loadedVersion) {
        logger.info("{} repeat version {}", newVersion.getCode(), newVersion.getVersion());
        getPersisterService().repeatVersion(newVersion, versionMapping, syncSourceService);
        dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo());
    }

    protected void addNewVersion(RefBookVersion newVersion, VersionMapping versionMapping) {
        logger.info("{} sync new version {}", newVersion.getCode(), newVersion.getVersion());
        LoadedVersion actualLoadedVersion = dao.getActualLoadedVersion(newVersion.getCode());
        if (newVersion.getFrom().isAfter(actualLoadedVersion.getPublicationDate())) {
            dao.closeLoadedVersion(actualLoadedVersion.getCode(), actualLoadedVersion.getVersion(), newVersion.getFrom());
        }
        dao.insertLoadedVersion(newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo(), newVersion.getFrom().isAfter(actualLoadedVersion.getPublicationDate()));
        getPersisterService().merge(newVersion, actualLoadedVersion.getVersion(), versionMapping, syncSourceService);
    }

    protected void addFirstVersion(RefBookVersion newVersion, VersionMapping versionMapping) {
        logger.info("{} first sync", newVersion.getCode());
        dao.insertLoadedVersion(newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo(), true);
        getPersisterService().firstWrite(newVersion, versionMapping, syncSourceService);
    }

}
