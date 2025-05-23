package ru.i_novus.ms.rdm.sync.init.dao;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Set;

public interface VersionMappingDao {

    List<VersionMapping> getVersionMappings();

    void deleteVersionMappings(Set<Integer> mappingIds);

    VersionMapping getVersionMappingByCodeAndRange(String referenceCode, String range);
}
