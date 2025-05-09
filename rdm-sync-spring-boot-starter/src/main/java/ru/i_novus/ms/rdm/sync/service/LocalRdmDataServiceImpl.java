package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDao;
import ru.i_novus.ms.rdm.sync.dao.criteria.DeletedCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

public class LocalRdmDataServiceImpl implements LocalRdmDataService {

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private VersionedDataDao versionedDataDao;

    @Autowired
    private VersionMappingService versionMappingService;

    @Override
    public Page<Map<String, Object>> getData(String refBookCode, Boolean getDeleted,
                                             Integer page, Integer size, UriInfo uriInfo) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, null);
        if (page == null) page = 0;
        if (size == null) size = 10;

        List<FieldFilter> filters = paramsToFilters(dao.getFieldMappings(versionMapping.getId()), uriInfo.getQueryParameters());

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(versionMapping.getTable(),
                versionMapping.getPrimaryField(), size, page * size, filters);

        DeletedCriteria deleted = new DeletedCriteria(versionMapping.getDeletedField(), Boolean.TRUE.equals(getDeleted));
        localDataCriteria.setDeleted(deleted);
        localDataCriteria.setSysPkColumn(versionMapping.getSysPkColumn());
        return dao.getData(localDataCriteria);
    }

    @Override
    public Page<Map<String, Object>> getVersionedData(String refBookCode, String version, Integer page, Integer size, UriInfo uriInfo) {
        SyncRefBook syncRefBook = dao.getSyncRefBook(refBookCode);

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, version);
        if (page == null) page = 0;
        if (size == null) size = 10;

        List<FieldFilter> filters = paramsToFilters(dao.getFieldMappings(versionMapping.getId()), uriInfo.getQueryParameters());
        VersionedLocalDataCriteria criteria = new VersionedLocalDataCriteria(refBookCode, versionMapping.getTable(),
                versionMapping.getPrimaryField(), size, page * size, filters, version);
        if (syncRefBook.getType().equals(SyncTypeEnum.VERSIONED)) {
            return versionedDataDao.getData(criteria);
        } else {
            return dao.getSimpleVersionedData(criteria);
        }
    }

    @Override
    public Map<String, Object> getSingle(String refBookCode, String primaryKey) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, null);
        FieldMapping fieldMapping = dao.getFieldMappings(versionMapping.getId()).stream()
                .filter(fm -> fm.getSysField().equals(versionMapping.getPrimaryField()))
                .findFirst().orElseThrow(() -> new RdmSyncException(versionMapping.getPrimaryField() + " not found in RefBook with code " + refBookCode));
        DataTypeEnum fieldType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());

        Serializable primaryValue = fieldType.toValue(primaryKey);
        FieldFilter primaryFilter = new FieldFilter(
                versionMapping.getPrimaryField(),
                fieldType,
                singletonList(new FieldValueFilter(FilterTypeEnum.EQUAL, singletonList(primaryValue)))
        );

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(versionMapping.getTable(),
                versionMapping.getPrimaryField(), 1, 0, singletonList(primaryFilter));
        Page<Map<String, Object>> page = dao.getData(localDataCriteria);

        return page.get().findAny().orElseThrow(NotFoundException::new);
    }

    /** Преобразование параметров запроса в список фильтров по полям. */
    private List<FieldFilter> paramsToFilters(List<FieldMapping> fieldMappings,
                                              MultivaluedMap<String, String> params) {

        Map<String, DataTypeEnum> fieldTypeMap = fieldMappings.stream()
                .collect(toMap(FieldMapping::getSysField, fm -> DataTypeEnum.getByDataType(fm.getSysDataType())));

        return params.entrySet().stream()
                .filter(param -> fieldTypeMap.get(param.getKey()) != null)
                .map(param -> paramToFilter(param, fieldTypeMap.get(param.getKey())))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @Override
    public Map<String, Object> getBySystemId(String refBookCode, Long recordId) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, null);

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(versionMapping.getTable(),
                versionMapping.getPrimaryField(), 1, 0, null);
        localDataCriteria.setRecordId(recordId);
        localDataCriteria.setSysPkColumn(versionMapping.getSysPkColumn());
        Page<Map<String, Object>> synced = dao.getData(localDataCriteria);

        return synced.get().findAny().orElseThrow(NotFoundException::new);
    }


    /** Преобразование параметра запроса в фильтр по полю. */
    private FieldFilter paramToFilter(Map.Entry<String, List<String>> param, DataTypeEnum fieldType) {

        Map<FilterTypeEnum, List<Object>> valueListMap = fieldType.toMap(param.getValue());
        List<FieldValueFilter> valueFilters = valueListMap.entrySet().stream()
                .filter(entry -> !isEmpty(entry.getValue()))
                .map(FieldValueFilter::new)
                .collect(toList());
        return !isEmpty(valueFilters) ? new FieldFilter(param.getKey(), fieldType, valueFilters) : null;
    }

    private VersionMapping getVersionMappingOrThrowRefBookNotFound(String refBookCode, String version) {

        VersionMapping versionMapping = versionMappingService.getVersionMapping(refBookCode, version);
        if (versionMapping == null)
            throw new RdmSyncException("RefBook with code '" + refBookCode + "' is not maintained in system.");

        return versionMapping;
    }
}
