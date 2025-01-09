package ru.i_novus.ms.rdm.sync.init.dao;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Set;

public interface VersionMappingDao {

    List<VersionMapping> getVersionMappings();

    void deleteVersionMappings(Set<Integer> mappingIds);
}
