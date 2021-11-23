package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.Map;

public class LocalRefBookCreatorLocator {

    private final Map<SyncTypeEnum, LocalRefBookCreator> creators;

    public LocalRefBookCreatorLocator(Map<SyncTypeEnum, LocalRefBookCreator> creators) {
        this.creators = creators;
    }

    public LocalRefBookCreator getLocalRefBookCreator(SyncTypeEnum type) {
        return creators.get(type);
    }

}
