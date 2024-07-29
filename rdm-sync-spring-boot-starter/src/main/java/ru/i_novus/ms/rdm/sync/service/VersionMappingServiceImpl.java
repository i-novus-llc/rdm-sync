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

        List<VersionMapping> versionMappings = rdmSyncDao.getVersionMappingsByRefBookCode(refBookCode);

        if (versionMappings.isEmpty()){
            return null;
        }

        //Сортируем
        List<VersionMapping> sortedVersionMappings = versionMappings.stream()
                .sorted(Comparator.comparing(VersionMapping::getRange))
                .collect(Collectors.toList());

        if (version != null){
            for (VersionMapping versionMapping : versionMappings) {

                //по версии найден маппинг
                if (versionMapping.getRange().containsVersion(version)) {
                    return versionMapping;
                }
            }
        }
        //версия равна null - берем последний
        if (version == null){
            return getLastVersionMapping(sortedVersionMappings);
        }
        //не найден ни один маппинг - возвращаем null
        return null;

    }

    private VersionMapping getLastVersionMapping(List<VersionMapping> sortedVersionMappings) {
        int lastVersionMappingIndex = sortedVersionMappings.size() - 1;
        return sortedVersionMappings.get(lastVersionMappingIndex);
    }
}
