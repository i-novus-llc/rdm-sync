package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class VersionMappingServiceImpl implements VersionMappingService {

    @Autowired
    private final RdmSyncDao rdmSyncDao;

    public VersionMappingServiceImpl(RdmSyncDao rdmSyncDao) {
        this.rdmSyncDao = rdmSyncDao;
    }

    @Override
    public VersionMapping getVersionMapping(String refBookCode, String version) {
        List<VersionMapping> versionMappings =
                rdmSyncDao.getVersionMappingsByRefBookCode(refBookCode)
                .stream().sorted(Comparator.comparing(VersionMapping::getRange))
                .collect(Collectors.toList());

        for (VersionMapping versionMapping : versionMappings) {
            if (versionMapping.getRange().containsVersion(version)) {
                return versionMapping;
            }
        }
        //todo
        return null;

    }
}
