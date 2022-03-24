package ru.i_novus.ms.rdm.sync.service.updater;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;

public class RefBookUpdaterException extends Exception {

    private final VersionMapping versionMapping;
    private final RefBookVersion newVersion;

    public RefBookUpdaterException(
        final Exception cause,
        final VersionMapping versionMapping,
        final RefBookVersion newVersion
    ) {
        super(cause);
        this.versionMapping = versionMapping;
        this.newVersion = newVersion;
    }

    public VersionMapping getVersionMapping() {
        return versionMapping;
    }

    public RefBookVersion getNewVersion() {
        return newVersion;
    }

}
