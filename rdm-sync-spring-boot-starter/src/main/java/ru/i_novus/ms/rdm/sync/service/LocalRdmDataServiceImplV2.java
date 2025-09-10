package ru.i_novus.ms.rdm.sync.service;

import jakarta.ws.rs.core.UriInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.LocalRdmDataServiceV2;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDao;
import ru.i_novus.ms.rdm.sync.dao.criteria.DeletedCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.rsql.RsqlToSqlConverter;

import java.util.Map;

public class LocalRdmDataServiceImplV2 implements LocalRdmDataServiceV2 {

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private VersionedDataDao versionedDataDao;

    @Autowired
    private VersionMappingService versionMappingService;

    @Override
    public Page<Map<String, Object>> getData(String refBookCode, Boolean getDeleted,
                                             Integer page, Integer size, String rsqlFilter, UriInfo uriInfo) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, null);
        if (page == null) page = 0;
        if (size == null) size = 10;

        String filterSql = RsqlToSqlConverter.convertToSql(rsqlFilter);

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(versionMapping.getTable(),
                versionMapping.getPrimaryField(), size, page * size, null);
        localDataCriteria.setFilterSql(filterSql);

        DeletedCriteria deleted = new DeletedCriteria(versionMapping.getDeletedField(), Boolean.TRUE.equals(getDeleted));
        localDataCriteria.setDeleted(deleted);
        localDataCriteria.setSysPkColumn(versionMapping.getSysPkColumn());
        return dao.getData(localDataCriteria);
    }

    @Override
    public Page<Map<String, Object>> getVersionedData(String refBookCode, String version, Integer page, Integer size, String rsqlFilter, UriInfo uriInfo) {
        SyncRefBook syncRefBook = dao.getSyncRefBook(refBookCode);

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, version);
        if (page == null) page = 0;
        if (size == null) size = 10;

        VersionedLocalDataCriteria criteria = new VersionedLocalDataCriteria(refBookCode, versionMapping.getTable(),
                versionMapping.getPrimaryField(), size, page * size, null, version);
        if (syncRefBook.getType().equals(SyncTypeEnum.VERSIONED)) {
            return versionedDataDao.getData(criteria);
        } else {
            return dao.getSimpleVersionedData(criteria);
        }
    }

    private VersionMapping getVersionMappingOrThrowRefBookNotFound(String refBookCode, String version) {

        VersionMapping versionMapping = versionMappingService.getVersionMapping(refBookCode, version);
        if (versionMapping == null)
            throw new RdmSyncException("RefBook with code '" + refBookCode + "' is not maintained in system.");

        return versionMapping;
    }
}
