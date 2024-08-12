package ru.i_novus.ms.rdm.sync.service.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.exception.MappingNotFoundException;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class DefaultRefBookUpdater implements RefBookUpdater {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRefBookUpdater.class);

    protected final RdmSyncDao dao;

    private final RdmLoggingService loggingService;

    protected final PersisterService persisterService;

    protected  final VersionMappingService versionMappingService;

    public DefaultRefBookUpdater(RdmSyncDao dao, RdmLoggingService loggingService, PersisterService persisterService, VersionMappingService versionMappingService) {
        this.dao = dao;
        this.loggingService = loggingService;
        this.persisterService = persisterService;
        this.versionMappingService = versionMappingService;
    }

    @Override
    @Transactional
    public void update(RefBookVersion refBookVersion, DownloadResult downloadResult) throws RefBookUpdaterException {
        logger.info("try to load {} version: {}", refBookVersion.getCode(), refBookVersion.getVersion());

        if (!refBookVersion.getStructure().hasPrimary()) {
            logger.error("Reference book with code '{}' has not primary key.", refBookVersion.getCode());
            return;
        }

        VersionMapping versionMapping;
        try {
            versionMapping = getVersionMapping(refBookVersion);
            logger.info("using version mapping with range: {} for refbook {} with version {}",
                    versionMapping.getRange(),
                    refBookVersion.getCode(),
                    refBookVersion.getVersion());

        } catch (Exception e) {
            logger.error("Error while fetching mapping for new version with code '{}'.", refBookVersion.getCode(), e);
            return;
        }


        LoadedVersion loadedVersion = dao.getLoadedVersion(refBookVersion.getCode(), refBookVersion.getVersion());
        String oldVersion = loadedVersion != null ? loadedVersion.getVersion() : null;
        try {//это надо перенести в RefBookVersionsDeterminator
            if (!dao.existsLoadedVersion(refBookVersion.getCode()) || loadedVersion == null || isMappingChanged(versionMapping, loadedVersion)
                    || (isNewVersionPublished(refBookVersion, loadedVersion)) && versionMapping.getType().equals(SyncTypeEnum.RDM_NOT_VERSIONED)) {

                update(refBookVersion, versionMapping, downloadResult);
                loggingService.logOk(refBookVersion.getCode(), oldVersion, refBookVersion.getVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refBookVersion.getCode());
            }
        } catch (final Exception e) {
            logger.error("cannot load {} version: {}", refBookVersion.getCode(), refBookVersion.getVersion());
            throw new RefBookUpdaterException(e, oldVersion, refBookVersion.getVersion());
        }
    }

    private VersionMapping getVersionMapping(RefBookVersion refBookVersion) {
        VersionMapping versionMapping = versionMappingService.getVersionMapping(refBookVersion.getCode(), refBookVersion.getVersion());
        if (versionMapping == null) {
            throw new MappingNotFoundException(refBookVersion.getCode(), refBookVersion.getCode());
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

    protected void update(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        logger.info("{} sync started", newVersion.getCode());
        // Если изменилась структура, проверяем актуальность полей в маппинге
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getId());
        validateStructureAndMapping(newVersion, fieldMappings, versionMapping.isMatchCase());
        boolean haveTrigger = dao.existsInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        if (haveTrigger) {
            dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }

        try {
            updateProcessing(newVersion, versionMapping, downloadResult);
        } finally {
            if (haveTrigger) {
                dao.enableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
            }
            dao.dropTable(downloadResult.getTableName());
        }
    }

    private void validateStructureAndMapping(RefBookVersion newVersion, List<FieldMapping> fieldMappings, boolean matchCase) {
        List<String> clientRdmFields = fieldMappings
                .stream()
                .filter(fieldMapping -> (!fieldMapping.getIgnoreIfNotExists() && fieldMapping.getDefaultValue() == null))
                .map(FieldMapping::getRdmField)
                .collect(toList());
        Set<String> actualFields = newVersion.getStructure().getAttributesAndTypes().keySet();
        Set<String> unknownClientRdmFields = new HashSet<>();
        clientRdmFields.forEach(clientRdmField -> {
            if((matchCase && !actualFields.contains(clientRdmField)) || (!matchCase && actualFields.stream().noneMatch(clientRdmField::equalsIgnoreCase)) ) {
                unknownClientRdmFields.add(clientRdmField);
            }
        });
        if (!unknownClientRdmFields.isEmpty()) {
            // В новой версии удалены поля, которые ведутся в системе
            throw new IllegalStateException(String.format("Field '%s' was deleted in version with code '%s'. Update your mappings.",
                    String.join(",", unknownClientRdmFields), newVersion.getCode()));
        }

    }

    private boolean isNewVersionPublished(RefBookVersionItem newVersion, LoadedVersion loadedVersion) {
        return loadedVersion.getPublicationDate().isBefore(newVersion.getFrom());
    }

    protected void updateProcessing(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion());
        if (loadedVersion == null && !dao.existsLoadedVersion(newVersion.getCode())) {
            addFirstVersion(newVersion, versionMapping, downloadResult);

        } else if (loadedVersion == null) {
            addNewVersion(newVersion, versionMapping, downloadResult);

        } else if (isMappingChanged(versionMapping, loadedVersion) || newVersion.getFrom().isAfter(loadedVersion.getPublicationDate())) {
            //Значит в прошлый раз мы синхронизировались по-старому маппингу.
            //Необходимо полностью залить свежую версию.
            editVersion(newVersion, versionMapping, loadedVersion, downloadResult);
        }

        logger.info("{}, version {} sync finished", newVersion.getCode(), newVersion.getVersion());
    }

    protected void editVersion(RefBookVersion newVersion, VersionMapping versionMapping, LoadedVersion loadedVersion, DownloadResult downloadResult) {
        logger.info("{} repeat version {}", newVersion.getCode(), newVersion.getVersion());
        persisterService.repeatVersion(newVersion, versionMapping, downloadResult);
        dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo());
    }

    protected void addNewVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        logger.info("{} sync new version {}", newVersion.getCode(), newVersion.getVersion());
        LoadedVersion actualLoadedVersion = dao.getActualLoadedVersion(newVersion.getCode());
        if (newVersion.getFrom().isAfter(actualLoadedVersion.getPublicationDate())) {
            dao.closeLoadedVersion(actualLoadedVersion.getCode(), actualLoadedVersion.getVersion(), newVersion.getFrom());
        }
        dao.insertLoadedVersion(newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo(), newVersion.getFrom().isAfter(actualLoadedVersion.getPublicationDate()));
        persisterService.merge(newVersion, actualLoadedVersion.getVersion(), versionMapping, downloadResult);
    }

    protected void addFirstVersion(RefBookVersion newVersion, VersionMapping versionMapping, DownloadResult downloadResult) {
        logger.info("{} first sync", newVersion.getCode());
        dao.insertLoadedVersion(newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), newVersion.getTo(), true);
        persisterService.firstWrite(newVersion, versionMapping, downloadResult);
    }

}
