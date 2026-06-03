package ru.i_novus.ms.rdm.sync.service;

import jakarta.ws.rs.core.UriInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LocalRdmDataServiceImplV2 implements LocalRdmDataServiceV2 {

    private static final String SORT_FIELD_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*";

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private VersionedDataDao versionedDataDao;

    @Autowired
    private VersionMappingService versionMappingService;

    @Override
    public Page<Map<String, Object>> getData(String refBookCode, Boolean getDeleted,
                                             Integer page, Integer size, String rsqlFilter, String sort, UriInfo uriInfo) {

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, null);
        if (page == null) page = 0;
        if (size == null) size = 10;

        String filterSql = RsqlToSqlConverter.convertToSql(rsqlFilter);

        LocalDataCriteria localDataCriteria = new LocalDataCriteria(versionMapping.getTable(),
                versionMapping.getPrimaryField(), size, page * size, null);
        localDataCriteria.setFilterSql(filterSql);
        localDataCriteria.setSortOrders(parseSortOrders(sort));

        DeletedCriteria deleted = new DeletedCriteria(versionMapping.getDeletedField(), Boolean.TRUE.equals(getDeleted));
        localDataCriteria.setDeleted(deleted);
        localDataCriteria.setSysPkColumn(versionMapping.getSysPkColumn());
        return dao.getData(localDataCriteria);
    }

    @Override
    public Page<Map<String, Object>> getVersionedData(String refBookCode, String version, Integer page, Integer size, String rsqlFilter, String sort, UriInfo uriInfo) {
        SyncRefBook syncRefBook = dao.getSyncRefBook(refBookCode);

        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode, version);
        if (page == null) page = 0;
        if (size == null) size = 10;

        String filterSql = RsqlToSqlConverter.convertToSql(rsqlFilter);
        VersionedLocalDataCriteria criteria = new VersionedLocalDataCriteria(refBookCode, versionMapping.getTable(),
                versionMapping.getPrimaryField(), size, page * size, null, version);
        criteria.setFilterSql(filterSql);
        criteria.setSortOrders(parseSortOrders(sort));
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

    /**
     * Parses a sort parameter of the form "field1:asc,field2:desc" into a list of Sort.Order.
     * Field names must be valid identifiers; direction defaults to ASC if omitted.
     * Throws RdmSyncException on invalid field name.
     */
    static List<Sort.Order> parseSortOrders(String sort) {
        if (sort == null || sort.isBlank()) return Collections.emptyList();
        List<Sort.Order> orders = new ArrayList<>();
        for (String part : sort.split(",")) {
            String[] tokens = part.trim().split(":");
            String field = tokens[0].trim();
            if (!field.matches(SORT_FIELD_PATTERN)) {
                throw new RdmSyncException("Invalid sort field name: '" + field + "'");
            }
            Sort.Direction direction = tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, field));
        }
        return orders;
    }
}
