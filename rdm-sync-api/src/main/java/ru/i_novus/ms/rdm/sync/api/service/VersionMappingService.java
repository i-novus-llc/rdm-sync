package ru.i_novus.ms.rdm.sync.api.service;

import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

public interface VersionMappingService {

    VersionMapping getVersionMapping(String referenceCode, String version);

}
