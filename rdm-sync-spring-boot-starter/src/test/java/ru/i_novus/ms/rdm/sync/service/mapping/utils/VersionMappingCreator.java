package ru.i_novus.ms.rdm.sync.service.mapping.utils;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

/**
 * Creator тестового {@link VersionMapping}
 */
public final class VersionMappingCreator {
    public static VersionMapping create(){
        return new VersionMapping(null, "EK003", "Справочник", null,
                "rdm.ref_ek003", "_sync_rec_id", "RDM", "id", "deleted_ts",
                null, -1, null, SyncTypeEnum.NOT_VERSIONED, null);
    }
}
