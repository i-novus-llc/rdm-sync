package ru.i_novus.ms.rdm.sync.service.updater;

import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.*;
import java.util.stream.Collectors;

public class RefBookVersionIterator implements Iterator<String> {

    private Iterator<String> versions;

    private List<VersionsRange> ranges = new ArrayList<>();


    public RefBookVersionIterator(SyncRefBook refBook, RdmSyncDao rdmSyncDao, SyncSourceService syncSourceService) {
        init(refBook, rdmSyncDao, syncSourceService);
    }

    @Override
    public boolean hasNext() {
        return versions.hasNext();
    }

    @Override
    public String next() {
        return versions.next();
    }

    private void init(SyncRefBook refBook, RdmSyncDao rdmSyncDao, SyncSourceService syncSourceService) {
        List<String> loadedVersions = rdmSyncDao.getLoadedVersions(refBook.getCode())
                .stream()
                .map(LoadedVersion::getVersion)
                .collect(Collectors.toList());
        List<RefBookVersion> allVersions = syncSourceService.getVersions(refBook.getCode());
        initRanges(refBook.getRange(), allVersions);
        this.versions = allVersions
                .stream()
                .filter(refBookVersion -> ranges.stream().anyMatch(range -> range.contains(refBookVersion)))
                .map(RefBookVersion::getVersion).filter(version -> !loadedVersions.contains(version))
                .collect(Collectors.toList()).iterator();
    }

    private void initRanges(String range, List<RefBookVersion> versions) {
        if (range.contains(",")) {
            Arrays.stream(range.split(",")).forEach(splitRange -> initRanges(splitRange, versions));
        } else if (range.contains("-")) {
            RefBookVersion left = null;
            RefBookVersion right = null;
            String[] splitRange = range.split("-");
            if (splitRange.length != 2) {
                throw new IllegalArgumentException("cannot parse " + range);
            }

            for (RefBookVersion version : versions) {
                if (version.getVersion().equals(splitRange[0]))
                    left = version;

                if (version.getVersion().equals(splitRange[1]))
                    right = version;
            }

            if (left != null || right != null) {
                ranges.add(new VersionsRange(left, right));
            }

        } else if (range.equals("*")) {
            ranges.add(new VersionsRange(null, null));
        } else {
            Optional<RefBookVersion> refBookVersion = versions.stream()
                    .filter(version -> version.getVersion().equals(range))
                    .findAny();
            refBookVersion.ifPresent(version -> ranges.add(new VersionsRange(version, version)));
        }
    }

    private static class VersionsRange {

        private RefBookVersion left;

        private RefBookVersion right;

        VersionsRange(RefBookVersion left, RefBookVersion right) {
            this.left = left;
            this.right = right;
        }

        boolean contains(RefBookVersion refBookVersion) {
            return
                    (left == null || !left.getFrom().isAfter(refBookVersion.getFrom()))
                            &&
                            (right == null || !right.getFrom().isBefore(refBookVersion.getFrom()));
        }

    }

}
