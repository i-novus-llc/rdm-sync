package ru.i_novus.ms.rdm.sync.service.persister;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.util.PageIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.i_novus.ms.rdm.sync.api.model.RowDiffStatusEnum.DELETED;
import static ru.i_novus.ms.rdm.sync.api.model.RowDiffStatusEnum.INSERTED;

/**
 * Хранит данные как актуальные и неактуальные, версионность отсутствует
 */
@Service
public class ActualDataPersisterService implements PersisterService {

    private static final Logger logger = LoggerFactory.getLogger(ActualDataPersisterService.class);

    private final RdmSyncDao dao;

    private final int maxSize;

    private final RdmMappingService mappingService;

    public ActualDataPersisterService(RdmSyncDao dao, @Value("${rdm.sync.load.size: 1000}") int maxSize, RdmMappingService mappingService) {
        this.maxSize = maxSize;
        this.dao = dao;
        this.mappingService = mappingService;
    }

    @Override
    public void firstWrite(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());


        final FieldMapping primaryField = fieldMappings.stream()
                .filter(mapping -> mapping.getSysField().equals(versionMapping.getPrimaryField()))
                .findFirst().orElse(null);
        List<Object> existingDataIds = dao.getDataIds(versionMapping.getTable(), primaryField);

        DataCriteria searchDataCriteria = new DataCriteria();
        searchDataCriteria.setCode(versionMapping.getCode());
        searchDataCriteria.setPageSize(maxSize);

        PageIterator<Map<String, ?>, DataCriteria> iter = new PageIterator<>(
                syncSourceService::getData, searchDataCriteria, true);
        while (iter.hasNext()) {
            Page<? extends Map<String, ?>> page = iter.next();

            insertOrUpdateRows(page.getContent(), existingDataIds, versionMapping, fieldMappings, newVersion);
            logProgress(versionMapping.getCode(), searchDataCriteria, page);

        }
    }

    @Override
    public void merge(RefBook newVersion, String synchedVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {
        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());
        VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(versionMapping.getCode(), newVersion.getLastVersion(), synchedVersion);
        VersionsDiff diff = syncSourceService.getDiff(versionsDiffCriteria);
        if (diff.isStructureChanged()) {

            dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
            firstWrite(newVersion, versionMapping, syncSourceService);

            return;
        }

        if (!diff.getRows().isEmpty()) {

            PageIterator<RowDiff, VersionsDiffCriteria> iter = new PageIterator<>(
                    criteria -> syncSourceService.getDiff(criteria).getRows(), versionsDiffCriteria, true);
            while (iter.hasNext()) {
                Page<? extends RowDiff> page = iter.next();
                for (RowDiff rowDiff : page.getContent()) {
                    mergeRow(newVersion, rowDiff, versionMapping, fieldMappings);
                }
            }
        }
    }



    @Override
    public void repeatVersion(RefBook newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {
        dao.markDeleted(versionMapping.getTable(), versionMapping.getDeletedField(), true, true);
        firstWrite(newVersion, versionMapping, syncSourceService);
    }

    private void mergeRow(RefBook newVersion, RowDiff rowDiff,
                          VersionMapping versionMapping, List<FieldMapping> fieldMappings) {

        Map<String, Object> mappedRow = new HashMap<>();
        for (Map.Entry<String, Object> entry : rowDiff.getRow().entrySet()){

            Map<String, Object> mappedValue = mapValue(newVersion,
                    entry.getKey(),
                    entry.getValue(),
                    fieldMappings);

            if (mappedValue != null) {
                mappedRow.putAll(mappedValue);
            }
        }

        final String table = versionMapping.getTable();
        final String primaryField = versionMapping.getPrimaryField();
        final Object primaryValue = mappedRow.get(primaryField);
        boolean idExists = dao.isIdExists(table, primaryField, primaryValue);

        if (DELETED.equals(rowDiff.getStatus())) {
            dao.markDeleted(table, primaryField, versionMapping.getDeletedField(), primaryValue, true, true);

        } else if (INSERTED.equals(rowDiff.getStatus()) && !idExists) {
            dao.insertRow(table, mappedRow, true);

        } else {
            dao.markDeleted(table, primaryField, versionMapping.getDeletedField(), primaryValue, false, true);
            dao.updateRow(table, primaryField, mappedRow, true);
        }
    }

    private void insertOrUpdateRows(List<? extends Map<String, ?>> rows, List<Object> existingDataIds,
                                    VersionMapping versionMapping, List<FieldMapping> fieldMappings, RefBook newVersion) {

        final String primaryField = versionMapping.getPrimaryField();

        List<Map<String, Object>> insertRows = new ArrayList<>();
        List<Map<String, Object>> updateRows = new ArrayList<>();

        for (Map<String, ?> row : rows) {
            Map<String, Object> mappedRow = new HashMap<>();
            for (Map.Entry<String, ?> fieldValue : row.entrySet()) {
                Map<String, Object> mappedValue = mapValue(newVersion, fieldValue.getKey(), fieldValue.getValue(), fieldMappings);
                if (mappedValue != null) {
                    mappedRow.putAll(mappedValue);
                }
            }

            //добавляем ключи со значение null которых нет в нси но есть маппинге
            // это важно для того чтобы размерность строки была фиксированна
            fieldMappings.forEach(mapping -> {
                if(!mappedRow.containsKey(mapping.getSysField())) {
                    mappedRow.put(mapping.getSysField(), null);
                }
            });


            final Object primaryValue = mappedRow.get(primaryField);
            if (existingDataIds.contains(primaryValue)) {
                // Если запись существует, то обновляем её:
                Map<String, Object> updatedRow = new HashMap<>(mappedRow);
                updatedRow.put(versionMapping.getDeletedField(), false);
                updateRows.add(updatedRow);

            } else {
                // Иначе - создаём новую запись:
                insertRows.add(mappedRow);
            }
        }
        if(!updateRows.isEmpty()) {
            dao.updateRows(versionMapping.getTable(), versionMapping.getPrimaryField(), updateRows, true);
        }
        if(!insertRows.isEmpty()) {
            dao.insertRows(versionMapping.getTable(), insertRows, true);
        }
    }

    private void logProgress(String refBookCode, RestCriteria criteria, Page currentPage) {
        int totalPages = currentPage.getContent().isEmpty() ? 1 : (int) Math.ceil((double) currentPage.getTotalElements() / (double) criteria.getPageSize());
        if(criteria.getPageNumber()%5==0) {
            logger.info("refbook {} {} rows of {} synchronized", refBookCode,  (criteria.getPageNumber() + 1)*criteria.getPageSize(), currentPage.getTotalElements());
        } else if (totalPages == criteria.getPageNumber() + 1) {
            logger.info("refbook {} {} rows of {} synchronized", refBookCode,  currentPage.getTotalElements(), currentPage.getTotalElements());
        }

    }

    private Map<String, Object> mapValue(RefBook newVersion, String rdmField, Object value,
                                         List<FieldMapping> fieldMappings) {

        FieldMapping fieldMapping = fieldMappings.stream()
                .filter(mapping -> mapping.getRdmField().equals(rdmField))
                .findAny().orElse(null);
        if (fieldMapping == null)
            return null; // Поле не ведётся в системе

        AttributeTypeEnum attributeType = newVersion.getStructure().getAttributesAndTypes().get(fieldMapping.getRdmField());
        DataTypeEnum clientType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());

        Map<String, Object> mappedValue = new HashMap<>();
        mappedValue.put(fieldMapping.getSysField(), mappingService.map(attributeType, clientType, value));

        return mappedValue;
    }
}
