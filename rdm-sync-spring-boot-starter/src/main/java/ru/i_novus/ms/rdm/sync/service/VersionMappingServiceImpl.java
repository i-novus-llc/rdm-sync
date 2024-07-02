package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;


@Service
public class VersionMappingServiceImpl implements VersionMappingService {

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Override
    public VersionMapping getVersionMapping(String referenceCode, String version) {
        //todo алгоритм поиска маппинга c учетом выбора из диапазона версий
        return null;
    }
}
