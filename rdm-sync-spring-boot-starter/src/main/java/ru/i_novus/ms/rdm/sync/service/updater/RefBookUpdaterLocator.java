package ru.i_novus.ms.rdm.sync.service.updater;

import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.Map;


public class RefBookUpdaterLocator {

    private final Map<SyncTypeEnum, RefBookUpdater> updaters;

    public RefBookUpdaterLocator(Map<SyncTypeEnum, RefBookUpdater> updaters) {
        this.updaters = updaters;
    }

    public RefBookUpdater getRefBookUpdater(SyncTypeEnum type) {
        RefBookUpdater refBookUpdater = updaters.get(type);
        if(refBookUpdater == null) {
            throw new IllegalArgumentException("cannot implement type " + type);
        }
        return refBookUpdater;
    }

}
