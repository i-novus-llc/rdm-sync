package ru.i_novus.ms.rdm.sync.service.mapping;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;

import java.util.List;

public interface MappingSourceService {

    List<SyncMapping> getMappings();

}
