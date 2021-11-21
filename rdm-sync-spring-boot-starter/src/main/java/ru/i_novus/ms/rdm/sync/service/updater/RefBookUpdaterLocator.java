package ru.i_novus.ms.rdm.sync.service.updater;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.Map;


public class RefBookUpdaterLocator {

    private final Map<SyncTypeEnum, RefBookUpdater> updaters;

    public RefBookUpdaterLocator(Map<SyncTypeEnum, RefBookUpdater> updaters) {
        this.updaters = updaters;
    }

    public RefBookUpdater getRefBookUpdater(SyncTypeEnum type) {
        return updaters.get(type);
    }

}
