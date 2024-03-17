package ru.i_novus.ms.rdm.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.impl.util.PageMapper;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.*;
import java.util.stream.Collectors;


public class  RdmSyncSourceService implements SyncSourceService {

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
    public RefBookVersion getRefBook(String code, String version) {
        final ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook;
        if (version != null) {
            rdmRefBook = versionService.getVersion(version, code);
        } else {
            RefBookCriteria refBookCriteria = new RefBookCriteria();
            refBookCriteria.setCode(code);
            refBookCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);

            Page<ru.i_novus.ms.rdm.api.model.refbook.RefBook> pageOfRdmRefBooks = refBookService.search(refBookCriteria);
            if (pageOfRdmRefBooks.getContent().size() > 1)
                throw new IllegalStateException(String.format(SEVERAL_REFBOOKS_WITH_CODE_FOUND, code));
            logger.info("refbook with code {} was found", code);
            if (pageOfRdmRefBooks.getContent().size() == 1) {
                rdmRefBook = pageOfRdmRefBooks.getContent().get(0);
            } else
                rdmRefBook = null;
        }

        if(rdmRefBook == null) {
            logger.warn("cannot find refbook by code {}", code);
            return null;
        }

        return convertToRefBookVersion(rdmRefBook);
    }



    @Override
    public List<RefBookVersionItem> getVersions(String code) {
        VersionCriteria versionCriteria = new VersionCriteria();
        versionCriteria.setRefBookCode(code);
        versionCriteria.setPageSize(Integer.MAX_VALUE);
        return versionService.getVersions(versionCriteria).getContent().stream()
                .map(this::convertToRefBookVersionItem).collect(Collectors.toList());
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        List<Map<String, Object>> data = new ArrayList<>();

        ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmVersion = versionService.getVersion(dataCriteria.getVersion(), dataCriteria.getCode());
        Page<Map<String, Object>> page = PageMapper.map(
                versionService.search(rdmVersion.getId(), new SearchDataCriteria(dataCriteria.getPageNumber(), dataCriteria.getPageSize())),
                refBookRowValue -> {
                    Map<String, Object> mapValue = new LinkedHashMap<>();
                    refBookRowValue.getFieldValues().stream()
                            .filter(fieldValue -> dataCriteria.getFields().contains(fieldValue.getField()))
                            .forEach(fieldVale -> mapValue.put(fieldVale.getField(), fieldVale.getValue()));
                    data.add(mapValue);
                    return mapValue;
                });
        return new PageImpl<>(data, dataCriteria, page.getTotalElements());
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
        compareDataCriteria.setPageSize(criteria.getPageSize());
        compareDataCriteria.setPageNumber(criteria.getPageNumber());
        RefBookDataDiff diff = compareService.compareData(compareDataCriteria);

        return VersionsDiff.dataChangedInstance(PageMapper.map(diff.getRows(), diffRowValue -> convert(diffRowValue, criteria)));
    }

    private RowDiff convert(DiffRowValue diffRowValue, VersionsDiffCriteria criteria) {
        if (diffRowValue == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        diffRowValue.getValues().stream().filter(diffFieldValue -> criteria.getFields().contains(diffFieldValue.getField().getName())).forEach(diffFieldValue -> {
            if (diffRowValue.getStatus().equals(DiffStatusEnum.DELETED)) {
                row.put(diffFieldValue.getField().getName(), diffFieldValue.getOldValue());
            } else {
                row.put(diffFieldValue.getField().getName(), diffFieldValue.getNewValue());
            }
        });

        return new RowDiff(RowDiffStatusEnum.valueOf(diffRowValue.getStatus().name()), row);

    }

    private RefBookVersion convertToRefBookVersion(ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook) {
        RefBookVersion refBook = new RefBookVersion();
        fillRefBookVersionItem(refBook, rdmRefBook);
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

    private RefBookVersionItem convertToRefBookVersionItem(ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook) {
        RefBookVersionItem refBookVersionItem = new RefBookVersionItem();
        fillRefBookVersionItem(refBookVersionItem, rdmRefBook);
        return refBookVersionItem;
    }

    private void fillRefBookVersionItem(RefBookVersionItem refBookVersionItem, ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook) {
        refBookVersionItem.setCode(rdmRefBook.getCode());
        refBookVersionItem.setFrom(rdmRefBook.getFromDate());
        refBookVersionItem.setVersion(rdmRefBook.getVersion());
        refBookVersionItem.setVersionId(rdmRefBook.getId());
    }
}
