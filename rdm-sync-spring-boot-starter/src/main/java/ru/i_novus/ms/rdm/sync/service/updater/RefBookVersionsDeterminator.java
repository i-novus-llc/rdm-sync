package ru.i_novus.ms.rdm.sync.service.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Определяет список версий подлежащих синхронизации
 */
public class RefBookVersionsDeterminator {

    private static final Logger logger = LoggerFactory.getLogger(RefBookVersionsDeterminator.class);

    private final SyncRefBook refBook;

    private final RdmSyncDao rdmSyncDao;

    private final SyncSourceService syncSourceService;

    private final VersionMappingService versionMappingService;

    public RefBookVersionsDeterminator(SyncRefBook refBook, RdmSyncDao rdmSyncDao, SyncSourceService syncSourceService, VersionMappingService versionMappingService) {
        this.refBook = refBook;
        this.rdmSyncDao = rdmSyncDao;
        this.syncSourceService = syncSourceService;
        this.versionMappingService = versionMappingService;
    }

    public List<String> getVersions() throws RefBookUpdaterException {

        List<LoadedVersion> loadedVersions = rdmSyncDao.getLoadedVersions(refBook.getCode());

        String actualLoadedVersion = null;
        try {
            for (LoadedVersion loadedVersion : loadedVersions ) {
                if(Boolean.TRUE.equals(loadedVersion.getActual())) {
                    actualLoadedVersion = loadedVersion.getVersion();
                }
            }
            Stream<RefBookVersionItem> refBookVersionStream;
            if(refBook.getRange() == null) {
                refBookVersionStream = Stream.of(syncSourceService.getRefBook(this.refBook.getCode(), null));
            } else {
                List<RefBookVersionItem> allVersions = syncSourceService.getVersions(refBook.getCode());
                List<VersionsRange> ranges = getRanges(refBook.getRange(), allVersions);
                refBookVersionStream =  allVersions.stream()
                        .filter(refBookVersion -> ranges.stream().anyMatch(range -> range.contains(refBookVersion)));
            }
            final String finalActualVersion = actualLoadedVersion;
            List<String> versions = refBookVersionStream
                    .filter(refBookVersion -> isNeedToLoad(refBookVersion, loadedVersions, finalActualVersion))
                    .map(RefBookVersionItem::getVersion)
                    .collect(Collectors.toList());
            if(versions.isEmpty()) {
                logger.info("there are no downloadable versions for refbook {}", refBook.getCode());
            }
            return versions;
        } catch (RuntimeException e) {
            logger.error("cannot get versions", e);
            throw new RefBookUpdaterException(e, actualLoadedVersion, null);
        }
    }

    private boolean isNeedToLoad(RefBookVersionItem refBookVersion, List<LoadedVersion> loadedVersions, String actualVersion) {
        VersionMapping versionMapping = versionMappingService.getVersionMapping(this.refBook.getCode(), refBookVersion.getVersion());
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
            logger.info("refresh refbook {} version {} because mapping is changed", refBook.getCode(), refBookVersion.getVersion());
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

    private List<VersionsRange> getRanges(Set<String> ranges, List<RefBookVersionItem> versions) {
        return ranges.stream()
                .map(range -> getRanges(range, versions))
                .flatMap(List::stream)
                .collect(Collectors.toList());


    }

    private List<VersionsRange> getRanges(String range, List<RefBookVersionItem> versions) {
        List<VersionsRange> result = new ArrayList<>();
        if (range.contains(",")) {
            Arrays.stream(range.split(",")).forEach(splitRange -> result.addAll(getRanges(splitRange, versions)));
        } else if (range.contains("-")) {
            RefBookVersionItem left = null;
            RefBookVersionItem right = null;
            String[] splitRange = range.split("-");
            if (splitRange.length != 2) {
                throw new IllegalArgumentException("cannot parse " + range);
            }

            for (RefBookVersionItem version : versions) {
                if (version.getVersion().equals(splitRange[0]))
                    left = version;

                if (version.getVersion().equals(splitRange[1]))
                    right = version;
            }

            if (left != null || right != null) {
                result.add(new VersionsRange(left, right));
            }

        } else if (range.equals("*")) {
            result.add(new VersionsRange(null, null));
        } else {
            Optional<RefBookVersionItem> refBookVersion = versions.stream()
                    .filter(version -> version.getVersion().equals(range))
                    .findAny();
            refBookVersion.ifPresent(version -> result.add(new VersionsRange(version, version)));
        }

        return result;
    }

    private static class VersionsRange {

        private final RefBookVersionItem left;

        private final RefBookVersionItem right;

        VersionsRange(RefBookVersionItem left, RefBookVersionItem right) {
            this.left = left;
            this.right = right;
        }

        boolean contains(RefBookVersionItem refBookVersion) {
            return
                    (left == null || !left.getFrom().isAfter(refBookVersion.getFrom()))
                            &&
                            (right == null || !right.getFrom().isBefore(refBookVersion.getFrom()));
        }

    }

}
