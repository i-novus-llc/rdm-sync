package ru.i_novus.ms.rdm.sync.service.mapping.utils;

import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.List;

/**
 * Creator тестового {@link VersionMapping и FieldMapping}
 */
public final class MappingCreator {
    public static VersionMapping createVersionMapping(){
        return new VersionMapping(null, "EK003", "Справочник", null,
                "rdm.ref_ek003", "_sync_rec_id", "RDM", "id", "deleted_ts",
                null, -1, null, SyncTypeEnum.NOT_VERSIONED, null);
    }

    public static List<FieldMapping> createFieldMapping() {
        return List.of(
                new FieldMapping("id", "integer", "id"),
                new FieldMapping("name", "varchar", "name")
        );
    }
}
