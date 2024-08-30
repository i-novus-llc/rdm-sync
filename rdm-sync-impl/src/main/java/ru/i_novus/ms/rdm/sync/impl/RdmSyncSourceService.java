package ru.i_novus.ms.rdm.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;

import java.util.*;


public class  RdmSyncSourceService implements SyncSourceService {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncSourceService.class);

    private static final String SEVERAL_REFBOOKS_WITH_CODE_FOUND =
            "Several reference books with code '%s' found.";

    private final String rdmUrl = "http://localhost:8081/rdm/api";

    private final RestClient restClient;

    public RdmSyncSourceService() {
        this.restClient = RestClient.create(rdmUrl);
    }

    @Override
    public RefBookVersion getRefBook(String code, String version) {

        if (version != null) {
            String uri = String.format("/version/%s/refbook/%s", version, code);
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(RefBookVersion.class);
        } else {
            RefBookCriteria refBookCriteria = new RefBookCriteria();
            refBookCriteria.setCodeExact(code);
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
       RefBookVersionItemList response = restClient.get()
                .uri("/version/versions?refBookCode={code}", code)
                .retrieve()
                .body(RefBookVersionItemList.class);

        return Objects.requireNonNull(response).getContent();
    }


    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        try {
            String uri = rdmUrl + "/data/search";
            return restClient.post()
                    .uri(uri)
                    .body(dataCriteria)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Page<Map<String, Object>>>() {});
        } catch (Exception e) {
            logger.error("Error fetching data: ", e);
            throw new RuntimeException("Failed to get data", e);
        }
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {
        try {
            String uri = rdmUrl + "/version/diff";
            return restClient.post()
                    .uri(uri)
                    .body(criteria)
                    .retrieve()
                    .body(VersionsDiff.class);
        } catch (Exception e) {
            logger.error("Error fetching versions diff: ", e);
            throw new RuntimeException("Failed to get versions diff", e);
        }
    }

//    private RowDiff convert(DiffRowValue diffRowValue, VersionsDiffCriteria criteria) {
//        if (diffRowValue == null) {
//            return null;
//        }
//        Map<String, Object> row = new LinkedHashMap<>();
//        diffRowValue.getValues().stream().filter(diffFieldValue -> criteria.getFields().contains(diffFieldValue.getField().getName())).forEach(diffFieldValue -> {
//            if (diffRowValue.getStatus().equals(DiffStatusEnum.DELETED)) {
//                row.put(diffFieldValue.getField().getName(), diffFieldValue.getOldValue());
//            } else {
//                row.put(diffFieldValue.getField().getName(), diffFieldValue.getNewValue());
//            }
//        });
//
//        return new RowDiff(RowDiffStatusEnum.valueOf(diffRowValue.getStatus().name()), row);
//
//    }
//
//    private RefBookVersion convertToRefBookVersion(ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook) {
//        RefBookVersion refBook = new RefBookVersion();
//        fillRefBookVersionItem(refBook, rdmRefBook);
//        RefBookStructure structure = new RefBookStructure();
//        structure.setAttributesAndTypes(new HashMap<>());
//        rdmRefBook.getStructure().getAttributes().forEach(attr -> {
//            structure.getAttributesAndTypes().put(attr.getCode(), AttributeTypeMapper.map(attr.getType()));
//            structure.setReferences(rdmRefBook.getStructure().getReferences().stream()
//                    .map(Structure.Reference::getReferenceCode)
//                    .collect(Collectors.toList()));
//            if (Boolean.TRUE.equals(attr.getIsPrimary())) {
//                if (structure.getPrimaries() == null) {
//                    structure.setPrimaries(new ArrayList<>());
//                }
//                structure.getPrimaries().add(attr.getCode());
//            }
//        });
//        refBook.setStructure(structure);
//        return refBook;
//    }
//
//    private RefBookVersionItem convertToRefBookVersionItem(ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook) {
//        RefBookVersionItem refBookVersionItem = new RefBookVersionItem();
//        fillRefBookVersionItem(refBookVersionItem, rdmRefBook);
//        return refBookVersionItem;
//    }
//
//    private void fillRefBookVersionItem(RefBookVersionItem refBookVersionItem, ru.i_novus.ms.rdm.api.model.version.RefBookVersion rdmRefBook) {
//        refBookVersionItem.setCode(rdmRefBook.getCode());
//        refBookVersionItem.setFrom(rdmRefBook.getFromDate());
//        refBookVersionItem.setVersion(rdmRefBook.getVersion());
//        refBookVersionItem.setVersionId(rdmRefBook.getId());
//    }
}
