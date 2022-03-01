package ru.i_novus.ms.rdm.sync.service.mapping;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;

import java.util.List;

public interface MappingSourceService {

    List<VersionAndFieldMapping> getVersionAndFieldMappingList();

}
