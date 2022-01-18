package ru.i_novus.ms.rdm.sync.service.updater;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.persister.PersisterService;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public abstract class BaseRefBookUpdater implements RefBookUpdater{

    private static final Logger logger = LoggerFactory.getLogger(BaseRefBookUpdater.class);

    protected final RdmSyncDao dao;

    protected final SyncSourceService syncSourceService;

    private final RdmLoggingService loggingService;

    protected abstract PersisterService getPersisterService();

    public BaseRefBookUpdater(RdmSyncDao dao, SyncSourceService syncSourceService, RdmLoggingService loggingService) {
        this.dao = dao;
        this.syncSourceService = syncSourceService;
        this.loggingService = loggingService;
    }

    @Override
    public void update(String refCode) {
        RefBookVersion newVersion;
        try {
            newVersion = getLastPublishedVersion(refCode);

        } catch (Exception e) {
            logger.error(String.format("Error while fetching new version with code '%s'.", refCode), e);
            return;
        }
        VersionMapping versionMapping = getVersionMapping(newVersion);
        if (versionMapping == null) {
            logger.error("No version mapping found for reference book with code '{}'.", refCode);
            return;
        }

        LoadedVersion loadedVersion = dao.getLoadedVersion(refCode);
        try {
            if (loadedVersion == null || isNewVersion(newVersion, loadedVersion) || isMappingChanged(versionMapping, loadedVersion)) {

                update(newVersion, versionMapping);
                loggingService.logOk(refCode, versionMapping.getVersion(), newVersion.getVersion());

            } else {
                logger.info("Skipping update on '{}'. No changes.", refCode);
            }
        } catch (Exception e) {
            logger.error(String.format("Error while updating new version with code '%s'.", refCode), e);

            loggingService.logError(refCode, versionMapping.getVersion(), newVersion.getVersion(),
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
        }

    }

    private RefBookVersion getLastPublishedVersion(String refBookCode) {
        RefBookVersion refBook = syncSourceService.getRefBook(refBookCode, null);
        if (refBook == null)
            throw new IllegalArgumentException(String.format("Reference book with code '%s' not found.", refBookCode));

        if (!refBook.getStructure().hasPrimary())
            throw new IllegalStateException(String.format("Reference book with code '%s' has not primary key.", refBookCode));
        return refBook;
    }

    private VersionMapping getVersionMapping(RefBookVersion refBookVersion) {
        VersionMapping versionMapping = dao.getVersionMapping(refBookVersion.getCode(), refBookVersion.getVersion());
        if(versionMapping == null) {
            versionMapping = dao.getVersionMapping(refBookVersion.getCode(), "CURRENT");
        }
        if (versionMapping == null) {
            return null;
        }
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());

        final String primaryField = versionMapping.getPrimaryField();
        if (fieldMappings.stream().noneMatch(mapping -> mapping.getSysField().equals(primaryField)))
            throw new IllegalArgumentException(String.format("No mapping found for primary key '%s'.", primaryField));

        return versionMapping;
    }

    /**
     * Проверяет новая ли версия грузится
     * @param newVersion версия которая грузится
     * @param loadedVersion последняя загруженная версия
     * @return если true, то новая, иначе версия уже была загруженна
     */
    protected abstract boolean isNewVersion(RefBookVersion newVersion, LoadedVersion loadedVersion);

    protected boolean isMappingChanged(VersionMapping versionMapping, LoadedVersion loadedVersion) {
        return versionMapping.getMappingLastUpdated().isAfter(loadedVersion.getLastSync());
    }

    protected void update(RefBookVersion newVersion, VersionMapping versionMapping) {
        logger.info("{} sync started", newVersion.getCode());
        // Если изменилась структура, проверяем актуальность полей в маппинге
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());
        validateStructureAndMapping(newVersion, fieldMappings);
        boolean haveTrigger = dao.existsInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        if (haveTrigger){
            dao.disableInternalLocalRowStateUpdateTrigger(versionMapping.getTable());
        }

        try {
            updateProcessing(newVersion, versionMapping);
        } catch (Exception e) {
            logger.error("cannot sync " + versionMapping.getCode(), e);
        } finally {
            if (haveTrigger){
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

    protected void updateProcessing(RefBookVersion newVersion, VersionMapping versionMapping) {
        LoadedVersion loadedVersion = dao.getLoadedVersion(newVersion.getCode());
        PersisterService persisterService = getPersisterService();
        if (loadedVersion == null) {
            //заливаем с нуля
            persisterService.firstWrite(newVersion, versionMapping, syncSourceService);

        } else if (isNewVersion(newVersion, loadedVersion)) {
            //если версия и дата публикация не совпадают - нужно обновить справочник
            persisterService.merge(newVersion, loadedVersion.getVersion(), versionMapping, syncSourceService);

        } else if (isMappingChanged(versionMapping, loadedVersion)) {
            //Значит в прошлый раз мы синхронизировались по старому маппингу.
            //Необходимо полностью залить свежую версию.
            persisterService.repeatVersion(newVersion, versionMapping, syncSourceService);
        }
        if (loadedVersion != null) {
            //обновляем версию в таблице версий клиента
            dao.updateLoadedVersion(loadedVersion.getId(), newVersion.getVersion(), newVersion.getFrom());
        } else {
            dao.insertLoadedVersion(newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom());
        }
        logger.info("{} sync finished", newVersion.getCode());
    }

}
