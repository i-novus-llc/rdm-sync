package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.dao.DeletedCriteria;
import ru.i_novus.ms.rdm.sync.dao.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.*;
import java.util.List;
import java.util.Map;

import static ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState.SYNCED;

@Service
public class LocalRdmDataServiceImpl implements LocalRdmDataService {

    @Autowired
    private RdmSyncDao dao;

    @Override
    public Page<Map<String, Object>> getData(String refBookCode, Boolean getDeleted,
                                             Integer page, Integer size, @Context UriInfo uriInfo) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode);
        if (getDeleted == null) getDeleted = false;
        if (page == null) page = 0;
        if (size == null) size = 10;

        MultivaluedMap<String, Object> filters = filtersToObjects(dao.getFieldMappings(refBookCode), uriInfo.getQueryParameters());
        LocalDataCriteria localDataCriteria =
                new LocalDataCriteria(versionMapping.getTable(),
                        versionMapping.getPrimaryField(),
                        size,
                        page * size,
                        SYNCED,
                        filters,
                        new DeletedCriteria(versionMapping.getDeletedField(), getDeleted));
        return dao.getData(localDataCriteria);
    }

    @Override
    public Map<String, Object> getSingle(String refBookCode, String pk) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode);
        FieldMapping fieldMapping = dao.getFieldMappings(refBookCode).stream().filter(fm -> fm.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElseThrow(() -> new RdmException(versionMapping.getPrimaryField() + " not found in RefBook with code " + refBookCode));
        DataTypeEnum dt = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());
        LocalDataCriteria localDataCriteria =
                new LocalDataCriteria(versionMapping.getTable(),
                        versionMapping.getPrimaryField(),
                        1,
                        0,
                        SYNCED,
                        new MultivaluedHashMap<>(Map.of(versionMapping.getPrimaryField(), dt.castFromString(pk))),
                        null);
        Page<Map<String, Object>> synced = dao.getData(localDataCriteria);
        return synced.get().findAny().orElseThrow(NotFoundException::new);
    }

    private MultivaluedMap<String, Object> filtersToObjects(List<FieldMapping> fieldMappings,
                                                            MultivaluedMap<String, String> filters) {

        MultivaluedMap<String, Object> res = new MultivaluedHashMap<>();
        for (MultivaluedMap.Entry<String, List<String>> e : filters.entrySet()) {
            fieldMappings.stream().filter(fm -> fm.getSysField().equals(e.getKey())).findAny().ifPresent(fm -> {
                DataTypeEnum dt = DataTypeEnum.getByDataType(fm.getSysDataType());
                if (dt != null) {
                    res.put(e.getKey(), (List<Object>) dt.castFromString(e.getValue()));
                }
            });
        }
        return res;
    }

    private VersionMapping getVersionMappingOrThrowRefBookNotFound(String refBookCode) {

        VersionMapping versionMapping = dao.getVersionMapping(refBookCode);
        if (versionMapping == null)
            throw new RdmException("RefBook with code '" + refBookCode + "' is not maintained in system.");

        return versionMapping;
    }
}
