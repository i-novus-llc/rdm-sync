package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RedirectingSyncSourceService implements SyncSourceService {

    private final RdmSyncDao syncDao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactorySet;

    private final VersionMappingService versionMappingService;

    @Autowired
    public RedirectingSyncSourceService(RdmSyncDao syncDao, Set<SyncSourceServiceFactory> syncSourceServiceFactorySet, VersionMappingService versionMappingService) {
        this.syncDao = syncDao;
        this.syncSourceServiceFactorySet = syncSourceServiceFactorySet;
        this.versionMappingService = versionMappingService;
    }

    @Override
    public RefBookVersion getRefBook(String code, String version) {
        RefBookVersion refBook = getSyncSourceService(code).getRefBook(code, version);
        //todo убрать когда в фнси появится отдельный тип данных для массива значений
        for (FieldMapping fieldMapping : getFieldMapping(code, refBook.getVersion())) {
            if ("integer[]".equals(fieldMapping.getSysDataType())) {
                refBook.getStructure().getAttributes().add(new RefBookStructure.Attribute(fieldMapping.getRdmField(), AttributeTypeEnum.INT_ARRAY, null));
            } else if ("text[]".equals(fieldMapping.getSysDataType())){
                refBook.getStructure().getAttributes().add(new RefBookStructure.Attribute(fieldMapping.getRdmField(), AttributeTypeEnum.STRING_ARRAY, null));
            }
        }
        return refBook;
    }

    @Override
    public List<RefBookVersionItem> getVersions(String code) {
        return getSyncSourceService(code).getVersions(code);
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        return getSyncSourceService(dataCriteria.getCode()).getData(dataCriteria);
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {
        return getSyncSourceService(criteria.getRefBookCode()).getDiff(criteria);
    }

    private SyncSourceService getSyncSourceService(String refBookCode) {
        SyncSource source = getSource(refBookCode);
        return syncSourceServiceFactorySet.stream().filter(factory ->
                factory.isSatisfied(source)).findAny().orElse(null).createService(source);
    }

    private SyncSource getSource(String refBookCode) {
        VersionMapping versionMapping = versionMappingService.getVersionMapping(refBookCode, null);
        return syncDao.findByCode(versionMapping.getSource());
    }

    //todo убрать когда в фнси появится отдельный тип данных для массива значений
    private List<FieldMapping> getFieldMapping(String code, String version) {
        VersionMapping versionMapping = versionMappingService.getVersionMapping(code,version);
        if (versionMapping == null) {
            return Collections.emptyList();
        }
        return syncDao.getFieldMappings(versionMapping.getId());
    }
}
