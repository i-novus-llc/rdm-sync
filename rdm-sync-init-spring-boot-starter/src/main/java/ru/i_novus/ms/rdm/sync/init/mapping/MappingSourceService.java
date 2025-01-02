package ru.i_novus.ms.rdm.sync.init.mapping;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;

import java.util.List;

public interface MappingSourceService {

    List<SyncMapping> getMappings();

}
