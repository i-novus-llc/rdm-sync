package ru.i_novus.ms.rdm.sync.service.downloader;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.persister.RetryingPageIterator;
import ru.i_novus.ms.rdm.sync.util.PageIterator;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RefBookDownloaderImpl implements RefBookDownloader {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDownloaderImpl.class);

    private final SyncSourceService syncSourceService;

    private final RdmSyncDao rdmSyncDao;

    private final int tries;

    private final int timeout;

    private final int maxSize;

    private final RdmMappingService rdmMappingService;

    private final VersionMappingService versionMappingService;

    public RefBookDownloaderImpl(SyncSourceService syncSourceService,
                                 RdmSyncDao rdmSyncDao,
                                 RdmMappingService rdmMappingService,
                                 @Value("${rdm-sync.load.retry.tries: 5}") int tries,
                                 @Value("${rdm-sync.load.retry.timeout: 30000}") int timeout,
                                 @Value("${rdm-sync.load.size: 1000}") int maxSize,
                                 VersionMappingService versionMappingService) {
        this.syncSourceService = syncSourceService;
        this.rdmSyncDao = rdmSyncDao;
        this.rdmMappingService = rdmMappingService;
        this.tries = tries;
        this.timeout = timeout;
        this.maxSize = maxSize;
        this.versionMappingService = versionMappingService;
    }

    @Override
    public DownloadResult download(String refCode, @Nullable String version) {
        String tempTableName = ("temp_" + refCode + "_" + version).replace(".", "_");
        rdmSyncDao.dropTable(tempTableName);
        VersionMapping versionMapping = versionMappingService.getVersionMapping(refCode, version);
        List<FieldMapping> fieldMappings = rdmSyncDao.getFieldMappings(versionMapping.getId());
        RefBookVersion refBookVersion = getRefBookVersion(refCode, version);
        DownloadResult downloadResult;
        if(rdmSyncDao.getLoadedVersion(refCode, version) == null && rdmSyncDao.existsLoadedVersion(refCode)
                && !SyncTypeEnum.SIMPLE_VERSIONED.equals(rdmSyncDao.getSyncRefBook(refCode).getType())
        ) {
            logger.info("trying to download diff for version {} of {}", version, refCode);
            downloadResult = downloadDiff(refBookVersion, tempTableName, versionMapping, fieldMappings);
        } else {
            logger.info("trying to download version {} of {}", version, refCode);
            downloadResult = downloadVersion(refBookVersion, tempTableName, versionMapping, fieldMappings);
        }
        logger.info("version {} of {} was downloaded", version, refCode);
        return downloadResult;
    }

    private DownloadResult downloadVersion(RefBookVersion refBookVersion, String tempTableName, VersionMapping versionMapping, List<FieldMapping> fieldMappings) {
        rdmSyncDao.createVersionTempDataTbl(tempTableName, versionMapping.getTable(), versionMapping.getSysPkColumn(), versionMapping.getPrimaryField());
        DataCriteria dataCriteria = new DataCriteria();
        dataCriteria.setFields(getUsesRefBookFields(fieldMappings, refBookVersion.getStructure(), versionMapping.isMatchCase()));
        dataCriteria.setVersion(refBookVersion.getVersion());
        dataCriteria.setCode(refBookVersion.getCode());
        dataCriteria.setPageSize(maxSize);
        dataCriteria.setRefBookStructure(refBookVersion.getStructure());

        RetryingPageIterator<Map<String, Object>> iter = new RetryingPageIterator<>(new PageIterator<>
                (syncSourceService::getData, dataCriteria, true),
                tries, timeout);
        Map<String, Object> defaultValues = getDefaultValues(refBookVersion, fieldMappings);
        while (iter.hasNext()) {
            Page<Map<String, Object>> nextPage = iter.next();
            rdmSyncDao.insertVersionAsTempData(tempTableName, nextPage.getContent().stream().map(row -> {
                if (!CollectionUtils.isEmpty(defaultValues)) {
                    row = new LinkedHashMap<>(row);
                    row.putAll(defaultValues);
                }
                return mapRow(row, refBookVersion, fieldMappings, versionMapping.isMatchCase());
            }).collect(Collectors.toList()));
            logProgress(refBookVersion.getCode(), dataCriteria, nextPage);
        }
        return new DownloadResult(tempTableName, DownloadResultType.VERSION);
    }

    private Map<String, Object> getDefaultValues(RefBookVersion refBookVersion, List<FieldMapping> fieldMappings) {
        return fieldMappings
                .stream()
                .filter(fieldMapping -> fieldMapping.getDefaultValue() != null && !refBookVersion.getStructure().getAttributesAndTypes().keySet().contains(fieldMapping.getRdmField()))
                .collect(Collectors.toMap(FieldMapping::getRdmField, FieldMapping::getDefaultValue));
    }

    private DownloadResult downloadDiff(RefBookVersion refBookVersion, String tempTableName, VersionMapping versionMapping, List<FieldMapping> fieldMappings) {
        LoadedVersion actualLoadedVersion = rdmSyncDao.getActualLoadedVersion(refBookVersion.getCode());
        Set<String> usesRefBookFields = getUsesRefBookFields(fieldMappings, refBookVersion.getStructure(), versionMapping.isMatchCase());
        VersionsDiffCriteria criteria = new VersionsDiffCriteria(refBookVersion.getCode(), refBookVersion.getVersion(), actualLoadedVersion.getVersion(), usesRefBookFields, refBookVersion.getStructure());
        criteria.setPageSize(maxSize);
        if(syncSourceService.getDiff(criteria).isStructureChanged()) {
            logger.info("structure refbook {} version {} was changed, downloading full version", refBookVersion.getCode(), refBookVersion.getVersion());
            return downloadVersion(refBookVersion,  tempTableName, versionMapping, fieldMappings);
        }
        rdmSyncDao.createDiffTempDataTbl(tempTableName, versionMapping.getTable());
        RetryingPageIterator<RowDiff> iter = new RetryingPageIterator<>(new PageIterator<>
                (diffCriteria -> syncSourceService.getDiff(diffCriteria).getRows(), criteria, true),
                tries, timeout);
        while (iter.hasNext()) {
            Page<? extends RowDiff> page = iter.next();
            List<Map<String, Object>> newRows = new ArrayList<>();
            List<Map<String, Object>> editedRows = new ArrayList<>();
            List<Map<String, Object>> deletedRows = new ArrayList<>();
            page.getContent().forEach(rowDiff -> {
                switch (rowDiff.getStatus()) {
                    case INSERTED:
                        newRows.add(mapRow(rowDiff.getRow(), refBookVersion, fieldMappings, versionMapping.isMatchCase()));
                        break;
                    case UPDATED:
                        editedRows.add(mapRow(rowDiff.getRow(), refBookVersion, fieldMappings, versionMapping.isMatchCase()));
                        break;
                    case DELETED:
                        deletedRows.add(mapRow(rowDiff.getRow(), refBookVersion, fieldMappings, versionMapping.isMatchCase()));
                        break;
                }
            });
            rdmSyncDao.insertDiffAsTempData(tempTableName, newRows, editedRows, deletedRows);
            logProgress(refBookVersion.getCode(), criteria, page);
        }
        return new DownloadResult(tempTableName, DownloadResultType.DIFF);
    }

    private Set<String> getUsesRefBookFields(List<FieldMapping> fieldMappings, RefBookStructure structure, boolean matchCase) {
        if (matchCase) {
            return fieldMappings.stream().map(FieldMapping::getRdmField).collect(Collectors.toSet());
        } else {
            return fieldMappings.stream()
                    .map(fieldMapping -> structure.getAttributesAndTypes().keySet().stream().filter(attr -> fieldMapping.getRdmField().equalsIgnoreCase(attr)).findAny().orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
    }


    private void logProgress(String refBookCode, RestCriteria criteria, Page currentPage) {
        int totalPages = currentPage.getContent().isEmpty() ? 1 : (int) Math.ceil((double) currentPage.getTotalElements() / (double) criteria.getPageSize());
        if (criteria.getPageNumber() % 5 == 0) {
            logger.info("refbook {} {} rows of {} downloaded", refBookCode, (criteria.getPageNumber()) * criteria.getPageSize() + currentPage.getContent().size(), currentPage.getTotalElements());
        } else if (totalPages == criteria.getPageNumber() + 1) {
            logger.info("refbook {} {} rows of {} downloaded", refBookCode, currentPage.getTotalElements(), currentPage.getTotalElements());
        }
    }

    /**
     * Конвертирует значения строки извне, в строку пригодную для структуры хранения.
     * Добавляет ключи со значение null которых нет в нси но есть маппинге, это важно для того чтобы размерность строки была фиксированна
     * @param row строка из источника данных
     * @return строка пригодная для сохранения
     */
    private Map<String, Object> mapRow(Map<String, ?> row, RefBookVersion refBookVersion, List<FieldMapping> fieldMappings, boolean matchCase) {
        Map<String, Object> mappedRow = new HashMap<>();
        for (Map.Entry<String, ?> fieldValue : row.entrySet()) {
            Map<String, Object> mappedValue = mapValue(refBookVersion, fieldValue.getKey(), fieldValue.getValue(), fieldMappings, matchCase);
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

        return  mappedRow;
    }

    private Map<String, Object> mapValue(RefBookVersion newVersion, String rdmField, Object value,
                                         List<FieldMapping> fieldMappings, boolean matchCase) {

        FieldMapping fieldMapping = fieldMappings.stream()
                .filter(mapping -> {
                    if(matchCase)
                        return mapping.getRdmField().equals(rdmField);
                    else
                        return mapping.getRdmField().equalsIgnoreCase(rdmField);
                })
                .findAny().orElse(null);
        if (fieldMapping == null)
            return null; // Поле не ведётся в системе

        AttributeTypeEnum attributeType = newVersion.getStructure().getAttributesAndTypes().get(rdmField);
        DataTypeEnum clientType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());

        Map<String, Object> mappedValue = new HashMap<>();
        mappedValue.put(fieldMapping.getSysField(), rdmMappingService.map(attributeType, clientType, value, fieldMapping.getTransformExpression()));

        return mappedValue;
    }

    private RefBookVersion getRefBookVersion(String refBookCode, String version) {
        RefBookVersion refBook = syncSourceService.getRefBook(refBookCode, version);
        if (refBook == null)
            throw new IllegalArgumentException(String.format("Reference book with code '%s' not found.", refBookCode));

        if (!refBook.getStructure().hasPrimary())
            throw new IllegalStateException(String.format("Reference book with code '%s' has not primary key.", refBookCode));
        return refBook;
    }

}
