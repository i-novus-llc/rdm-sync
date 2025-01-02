package ru.i_novus.ms.rdm.sync.service.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMappingComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Определяет список версий подлежащих синхронизации
 */
public class RefBookVersionsDeterminator {

    private static final Logger logger = LoggerFactory.getLogger(RefBookVersionsDeterminator.class);

    private final String refBookCode;

    private final RdmSyncDao rdmSyncDao;

    private final SyncSourceService syncSourceService;

    private final VersionMappingService versionMappingService;

    public RefBookVersionsDeterminator(String refBookCode, RdmSyncDao rdmSyncDao, SyncSourceService syncSourceService, VersionMappingService versionMappingService) {
        this.refBookCode = refBookCode;
        this.rdmSyncDao = rdmSyncDao;
        this.syncSourceService = syncSourceService;
        this.versionMappingService = versionMappingService;
    }

    public List<String> getVersions() throws RefBookUpdaterException {

        List<LoadedVersion> loadedVersions = rdmSyncDao.getLoadedVersions(refBookCode);
        List<VersionMapping> versionMappings = rdmSyncDao.getVersionMappingsByRefBookCode(refBookCode);

        String actualLoadedVersion = null;
        try {
            for (LoadedVersion loadedVersion : loadedVersions ) {
                if(Boolean.TRUE.equals(loadedVersion.getActual())) {
                    actualLoadedVersion = loadedVersion.getVersion();
                }
            }
            //sort by
            Stream<RefBookVersionItem> refBookVersionStream = versionMappings.stream().sorted(new VersionMappingComparator()).flatMap(this::versionMappingToRefBookVersion);

            final String finalActualVersion = actualLoadedVersion;
            List<String> versions = refBookVersionStream
                    .filter(refBookVersion -> isNeedToLoad(refBookVersion, loadedVersions, finalActualVersion))
                    .map(RefBookVersionItem::getVersion)
                    .collect(Collectors.toList());
            if(versions.isEmpty()) {
                logger.info("there are no downloadable versions for refbook {}", refBookCode);
            }
            return versions;
        } catch (RuntimeException e) {
            logger.error("cannot get versions", e);
            throw new RefBookUpdaterException(e, actualLoadedVersion, null);
        }
    }

    private Stream<? extends RefBookVersionItem> versionMappingToRefBookVersion(VersionMapping versionMapping) {
        if(versionMapping.getRange() == null || versionMapping.getRange().getRange() == null) {
            return Stream.of(syncSourceService.getRefBook(this.refBookCode, null));
        } else {
            List<RefBookVersionItem> allVersions = syncSourceService.getVersions(refBookCode);
            return allVersions.stream()
                    .filter(refBookVersion -> versionMapping.getRange().containsVersion(refBookVersion.getVersion()));
        }
    }


    private boolean isNeedToLoad(RefBookVersionItem refBookVersion, List<LoadedVersion> loadedVersions, String actualVersion) {
        VersionMapping versionMapping = versionMappingService.getVersionMapping(this.refBookCode, refBookVersion.getVersion());
        List<String> loadedVersionsStringValues = new ArrayList<>();
        LoadedVersion currentLoadedVersion = null;
        for (LoadedVersion loadedVersion : loadedVersions ) {
            loadedVersionsStringValues.add(loadedVersion.getVersion());
            if(loadedVersion.getVersion().equals(refBookVersion.getVersion())) {
                currentLoadedVersion = loadedVersion;
            }
        }
        boolean isNewVersion = !loadedVersionsStringValues.contains(refBookVersion.getVersion());
        boolean isMappingChanged = currentLoadedVersion != null && !versionMapping.getMappingLastUpdated().isBefore(currentLoadedVersion.getLastSync());
        if(isMappingChanged && versionMapping.isRefreshableRange()) {
            logger.info("refresh refbook {} version {} because mapping is changed", refBookCode, refBookVersion.getVersion());
            return true;
        }
        // изменился неверсионный справочник в rdm
        boolean isRdmNotVersionedRefBookChanged = currentLoadedVersion != null && isNewVersionPublished(refBookVersion, currentLoadedVersion) && versionMapping.getType().equals(SyncTypeEnum.RDM_NOT_VERSIONED);
        boolean result =  isNewVersion || (isMappingChanged && refBookVersion.getVersion().equals(actualVersion)) || isRdmNotVersionedRefBookChanged;
        if(!result) {
            logger.info("skip update refbook {} version {}", refBookVersion.getCode(), refBookVersion.getVersion());
        }
        return result;
    }

    private boolean isNewVersionPublished(RefBookVersionItem newVersion, LoadedVersion loadedVersion) {
        return loadedVersion.getPublicationDate().isBefore(newVersion.getFrom());
    }

}
