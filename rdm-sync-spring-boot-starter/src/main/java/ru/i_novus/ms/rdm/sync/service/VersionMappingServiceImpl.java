package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.util.VersionMappingComparator;

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

        List<VersionMapping> versionMappings = rdmSyncDao.getVersionMappingsByRefBookCode(refBookCode);

        if (versionMappings.isEmpty()){
            return null;
        }

        //Сортируем
        List<VersionMapping> sortedVersionMappings = versionMappings.stream()
                .sorted(new VersionMappingComparator())
                .collect(Collectors.toList());

        if (version != null){
            VersionMapping defaultMapping = null;
            for (VersionMapping versionMapping : versionMappings) {

                //по версии найден маппинг
                if (versionMapping.getRange()!= null && versionMapping.getRange().containsVersion(version)) {
                    return versionMapping;
                }
                if(versionMapping.getRange() == null) {
                    defaultMapping = versionMapping;
                }
            }
            return defaultMapping;
        }
        //версия равна null - берем последний
        return getLastVersionMapping(sortedVersionMappings);


    }

    @Override
    public VersionMapping getVersionMappingByCodeAndRange(String referenceCode, String range) {
        return rdmSyncDao.getVersionMappingByRefBookCodeAndRange(referenceCode, range);
    }

    private VersionMapping getLastVersionMapping(List<VersionMapping> sortedVersionMappings) {
        int lastVersionMappingIndex = sortedVersionMappings.size() - 1;
        return sortedVersionMappings.get(lastVersionMappingIndex);
    }

}
