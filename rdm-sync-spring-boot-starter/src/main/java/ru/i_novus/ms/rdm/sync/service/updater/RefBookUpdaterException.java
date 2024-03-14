package ru.i_novus.ms.rdm.sync.service.updater;

public class RefBookUpdaterException extends Exception {

    private final String oldVersion;
    private final String newVersion;

    public RefBookUpdaterException(
        final Exception cause,
        final String oldVersion,
        final String newVersion
    ) {
        super(cause);
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }
}
