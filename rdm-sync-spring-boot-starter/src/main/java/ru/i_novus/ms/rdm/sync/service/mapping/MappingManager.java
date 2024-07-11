package ru.i_novus.ms.rdm.sync.service.mapping;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;

public interface MappingManager {

    List<VersionMapping> validateAndGetMappingsToUpdate(List<SyncMapping> mappings);
}
