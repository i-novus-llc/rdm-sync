package ru.i_novus.ms.rdm.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.impl.util.PageMapper;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class RdmSyncSourceService implements SyncSourceService {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncSourceService.class);

    private static final String SEVERAL_REFBOOKS_WITH_CODE_FOUND =
            "Several reference books with code '%s' found.";

    private final RefBookService refBookService;

    private final VersionRestService versionService;

    private final CompareService compareService;


    public RdmSyncSourceService(RefBookService refBookService, VersionRestService versionService, CompareService compareService) {
        this.refBookService = refBookService;
        this.versionService = versionService;
        this.compareService = compareService;
    }

    @Override
    public RefBook getRefBook(String code) {
        RefBookCriteria refBookCriteria = new RefBookCriteria();
        refBookCriteria.setCode(code);
        Page<ru.i_novus.ms.rdm.api.model.refbook.RefBook> pageOfRdmRefBooks = refBookService.search(refBookCriteria);
        if (pageOfRdmRefBooks.isEmpty()) {
            logger.warn("cannot find refbook by code {}", code);
            return null;
        }
        if (pageOfRdmRefBooks.getContent().size() > 1)
            throw new IllegalStateException(String.format(SEVERAL_REFBOOKS_WITH_CODE_FOUND, code));
        logger.info("refbook with code {} was found", code);
        ru.i_novus.ms.rdm.api.model.refbook.RefBook rdmRefBook = pageOfRdmRefBooks.getContent().get(0);
        RefBook refBook = new RefBook();
        refBook.setCode(code);
        refBook.setLastPublishDate(rdmRefBook.getLastPublishedVersionFromDate());
        refBook.setLastVersion(rdmRefBook.getLastPublishedVersion());
        refBook.setLastVersionId(rdmRefBook.getId());
        RefBookStructure structure = new RefBookStructure();
        structure.setAttributesAndTypes(new HashMap<>());
        rdmRefBook.getStructure().getAttributes().forEach(attr -> {
            structure.getAttributesAndTypes().put(attr.getCode(), AttributeTypeMapper.map(attr.getType()));
            structure.setReferences(rdmRefBook.getStructure().getReferences().stream()
                    .map(Structure.Reference::getReferenceCode)
                    .collect(Collectors.toList()));
            if (Boolean.TRUE.equals(attr.getIsPrimary())) {
                if (structure.getPrimaries() == null) {
                    structure.setPrimaries(new ArrayList<>());
                }
                structure.getPrimaries().add(attr.getCode());
            }
        });
        refBook.setStructure(structure);
        return refBook;
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        return versionService.search(dataCriteria.getCode(), new SearchDataCriteria(dataCriteria.getPageNumber(), dataCriteria.getPageSize())).map(refBookRowValue -> {
            Map<String, Object> mapValue = new LinkedHashMap<>();
            refBookRowValue.getFieldValues().forEach(fieldVale -> mapValue.put(fieldVale.getField(), fieldVale.getValue()));
            return mapValue;
        });

    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {
        Integer oldVersionId = versionService.getVersion(criteria.getOldVersion(), criteria.getRefBookCode()).getId();
        Integer newVersionId = versionService.getVersion(criteria.getNewVersion(), criteria.getRefBookCode()).getId();

        StructureDiff structureDiff = compareService.compareStructures(oldVersionId, newVersionId);
        if (!CollectionUtils.isEmpty(structureDiff.getUpdated()) || !CollectionUtils.isEmpty(structureDiff.getDeleted()) || !CollectionUtils.isEmpty(structureDiff.getInserted())) {
            return VersionsDiff.structureChangedInstance();
        }
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria();
        compareDataCriteria.setOldVersionId(oldVersionId);
        compareDataCriteria.setNewVersionId(newVersionId);
        compareDataCriteria.setPageSize(100);
        compareDataCriteria.setPageNumber(criteria.getPageNumber());
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);

        return VersionsDiff.dataChangedInstance(PageMapper.map(diff.getRows(), this::convert));
    }

    private RowDiff convert(DiffRowValue diffRowValue) {
        if (diffRowValue == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        diffRowValue.getValues().forEach(diffFieldValue -> {
            if (diffRowValue.getStatus().equals(DiffStatusEnum.DELETED)) {
                row.put(diffFieldValue.getField().getName(), diffFieldValue.getOldValue());
            } else {
                row.put(diffFieldValue.getField().getName(), diffFieldValue.getNewValue());
            }
        });

        return new RowDiff(RowDiffStatusEnum.valueOf(diffRowValue.getStatus().name()), row);

    }


}
