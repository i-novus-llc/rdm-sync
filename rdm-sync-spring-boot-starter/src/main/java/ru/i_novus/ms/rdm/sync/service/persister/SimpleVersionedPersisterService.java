package ru.i_novus.ms.rdm.sync.service.persister;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.util.PageIterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class SimpleVersionedPersisterService implements PersisterService {

    private final RdmSyncDao rdmSyncDao;

    private final int maxSize;

    private final RdmMappingService mappingService;

    private final int tries;

    private final int timeout;

    public SimpleVersionedPersisterService(RdmSyncDao rdmSyncDao,
                                           @Value("${rdm-sync.load.size: 1000}") int maxSize,
                                           RdmMappingService mappingService,
                                           @Value("${rdm-sync.load.retry.tries: 5}") int tries,
                                           @Value("${rdm-sync.load.retry.tries: 30000}") int timeout) {
        this.rdmSyncDao = rdmSyncDao;
        this.maxSize = maxSize;
        this.mappingService = mappingService;
        this.timeout = timeout;
        this.tries = tries;
    }

    @Override
    public void firstWrite(RefBookVersion newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {
        List<FieldMapping> fieldMappings = rdmSyncDao.getFieldMappings(versionMapping.getId());
        DataCriteria searchDataCriteria = new DataCriteria();
        searchDataCriteria.setCode(versionMapping.getCode());
        searchDataCriteria.setVersion(newVersion.getVersion());
        searchDataCriteria.setPageSize(maxSize);
        insertVersion(newVersion, versionMapping, syncSourceService, fieldMappings, searchDataCriteria);

    }

    @Override
    public void merge(RefBookVersion newVersion, String synchedVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {
        //RefBookVersion oldVersion = syncSourceService.getRefBook(versionMapping.getCode(), synchedVersion);
       /* if(oldVersion.getTo() == null) {
            throw new IllegalStateException("old version " + synchedVersion + " of refbook " + versionMapping.getCode() + " has empty close date");
        }*/
        List<FieldMapping> fieldMappings = rdmSyncDao.getFieldMappings(versionMapping.getId());
        DataCriteria searchDataCriteria = new DataCriteria();
        searchDataCriteria.setCode(versionMapping.getCode());
        searchDataCriteria.setVersion(newVersion.getVersion());
        searchDataCriteria.setPageSize(maxSize);
        insertVersion(newVersion, versionMapping, syncSourceService, fieldMappings, searchDataCriteria);
    }

    @Override
    public void repeatVersion(RefBookVersion refBookVersion, VersionMapping versionMapping, SyncSourceService syncSourceService) {
        DataCriteria searchDataCriteria = new DataCriteria();
        searchDataCriteria.setCode(versionMapping.getCode());
        searchDataCriteria.setVersion(refBookVersion.getVersion());
        searchDataCriteria.setVersion(refBookVersion.getVersion());
        searchDataCriteria.setPageSize(maxSize);
        updateVersion(refBookVersion, versionMapping, syncSourceService, rdmSyncDao.getFieldMappings(versionMapping.getId()), searchDataCriteria);
    }

    private void insertVersion(RefBookVersion newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService, List<FieldMapping> fieldMappings, DataCriteria searchDataCriteria) {
        processRows(newVersion, syncSourceService, fieldMappings, searchDataCriteria,
                rows -> rdmSyncDao.insertSimpleVersionedRows(versionMapping.getTable(), rows, rdmSyncDao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion()).getId()));
    }

    private void updateVersion(RefBookVersion newVersion, VersionMapping versionMapping, SyncSourceService syncSourceService, List<FieldMapping> fieldMappings, DataCriteria searchDataCriteria) {
        processRows(newVersion, syncSourceService, fieldMappings, searchDataCriteria,
                rows -> rdmSyncDao.upsertVersionedRows(versionMapping.getTable(), rows, rdmSyncDao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion()).getId(), versionMapping.getPrimaryField()));
    }

    private void processRows(RefBookVersion newVersion, SyncSourceService syncSourceService,
                             List<FieldMapping> fieldMappings, DataCriteria searchDataCriteria, Consumer<List<Map<String, Object>>> rowProcessing) {
        RetryingPageIterator iter = new RetryingPageIterator<>(new PageIterator<>(
                syncSourceService::getData, searchDataCriteria, true), tries, timeout);
        while (iter.hasNext()) {
            Page<? extends Map<String, Object>> page = iter.next();
            List<Map<String, Object>> mappedRows = page.getContent().stream().map(row -> mapRow(row, newVersion, fieldMappings)).collect(Collectors.toList());
            rowProcessing.accept(mappedRows);
        }
    }


    /**
     * Конвертирует значения строки извне, в строку пригодную для структуры хранения.
     * Добавляет ключи со значение null которых нет в нси но есть маппинге, это важно для того чтобы размерность строки была фиксированна
     * @param row строка из источника данных
     * @return строка пригодная для сохранения
     */
    private Map<String, Object> mapRow(Map<String, ?> row, RefBookVersion refBookVersion, List<FieldMapping> fieldMappings) {
        Map<String, Object> mappedRow = new HashMap<>();
        for (Map.Entry<String, ?> fieldValue : row.entrySet()) {
            Map<String, Object> mappedValue = mapValue(refBookVersion, fieldValue.getKey(), fieldValue.getValue(), fieldMappings);
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

        LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(refBookVersion.getCode(), refBookVersion.getVersion());
        return  mappedRow;
    }

    private Map<String, Object> mapValue(RefBookVersion newVersion, String rdmField, Object value,
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
