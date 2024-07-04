package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.updater.RefBookVersionsDeterminator;

import java.util.List;


@Service
public class VersionMappingServiceImpl implements VersionMappingService {

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Override
    public VersionMapping getVersionMapping(String refBookCode, String version) {
        //todo алгоритм поиска маппинга c учетом выбора из диапазона версий
        List<VersionMapping> versionMappings = rdmSyncDao.getVersionMappingsByRefBookCode(refBookCode);

        return null;
    }
}
