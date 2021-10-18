package ru.i_novus.ms.fnsi.sync.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FnsiSyncSourceService implements SyncSourceService {

    private static final Logger logger = LoggerFactory.getLogger(FnsiSyncSourceService.class);

    private final RestTemplate restTemplate;

    private final String fnsiUrl;

    private final String userKey;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public FnsiSyncSourceService(RestTemplate restTemplate, String fnsiUrl, String userKey) {
        this.restTemplate = restTemplate;
        this.fnsiUrl = fnsiUrl;
        this.userKey = userKey;
    }

    @Override
    public RefBook getRefBook(String code) {
        JsonNode response = requestRefBook(code);
        JsonNode listRefBookNodes = response.get("list");
        if(listRefBookNodes.isEmpty()) {
            return null;
        }
        JsonNode refBookNode = listRefBookNodes.get(0);
        RefBook refBook = new RefBook();
        refBook.setLastPublishDate(LocalDateTime.parse(refBookNode.get("publishDate").asText(), formatter));
        refBook.setLastVersion(refBookNode.get("version").asText());
        refBook.setCode(code);
        RefBookStructure refBookStructure = getRefBookStructure(code, refBook.getLastVersion());
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        RefBook refBook = getRefBook(dataCriteria.getCode());
        JsonNode jsonNode = requestData(dataCriteria.getCode(), dataCriteria.getPageNumber() + 1, dataCriteria.getPageSize());
        List<Map<String, Object>> data = new ArrayList<>();
        jsonNode.get("list").elements().forEachRemaining(itemNode -> {
            Map<String, Object> row = new LinkedHashMap<>();
            itemNode.elements().forEachRemaining(cellNode -> {
                String value = cellNode.get("value").asText();
                if (!"null".equals(value.trim())) {
                    String column = cellNode.get("column").asText();
                    AttributeTypeEnum attributeTypeEnum = refBook.getStructure().getAttributesAndTypes().get(column);
                    row.put(column, attributeTypeEnum.castValue(value));
                }
            });
            data.add(row);
        });

        return new PageImpl<>(data, dataCriteria, jsonNode.get("total").asInt());
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        Iterator<JsonNode> versionNodeIterator = requestVersions(criteria.getRefBookCode()).get("list").elements();
        while (versionNodeIterator.hasNext() && (fromDate == null || toDate == null)) {
            JsonNode versionNode = versionNodeIterator.next();
            if(versionNode.get("version").asText().equals(criteria.getOldVersion())) {
                fromDate = LocalDateTime.parse(versionNode.get("publishDate").asText(), formatter);
            } else if (versionNode.get("version").asText().equals(criteria.getNewVersion())) {
                toDate = LocalDateTime.parse(versionNode.get("publishDate").asText(), formatter);
            }
        }
        JsonNode jsonNode = requestDiff(criteria.getRefBookCode(), fromDate, toDate, criteria.getPageNumber() + 1);
        if("ERROR".equals(jsonNode.get("result").asText().trim())) {
            return getErrorDiff(jsonNode);
        }
        if(!"null".equals(jsonNode.get("fields").asText().trim())) {
            return VersionsDiff.structureChangedInstance();
        }
        JsonNode diffsNode = jsonNode.get("data").get("list");
        List<RowDiff> rowDiffList = new ArrayList<>();
        RefBookStructure refBookStructure = getRefBookStructure(criteria.getRefBookCode(), criteria.getNewVersion());
        diffsNode.elements().forEachRemaining(diffNode -> {
            Map<String, AttributeTypeEnum> attributesAndTypes = refBookStructure.getAttributesAndTypes();
            Map<String, Object> row = new HashMap<>();
            for (Map.Entry<String, AttributeTypeEnum> entry : attributesAndTypes.entrySet()) {
                JsonNode nodeValue = diffNode.get(entry.getKey());
                if(nodeValue.asText().trim().equals("null")) {
                    continue;
                }
                row.put(entry.getKey(), entry.getValue().castValue(nodeValue.asText()));
            }
            RowDiff rowDiff = new RowDiff(getRowDiffStatusEnum(diffNode.get("operation").asText()), row);
            rowDiffList.add(rowDiff);
        });
        return VersionsDiff.dataChangedInstance(new PageImpl<>(rowDiffList));
    }

    private VersionsDiff getErrorDiff(JsonNode jsonNode) {
        if("03x0002".equals(jsonNode.get("resultCode").asText().trim())){
            return VersionsDiff.dataChangedInstance(Page.empty());
        } else {
            throw new FnsiErrorException(jsonNode.get("resultCode").asText("resultText"));
        }
    }

    private RowDiffStatusEnum getRowDiffStatusEnum(String fnsiOperation){
        switch (fnsiOperation) {
            case "UPDATE" : return RowDiffStatusEnum.UPDATED;
            case "DELETE" : return RowDiffStatusEnum.DELETED;
            case "INSERT" : return RowDiffStatusEnum.INSERTED;
        }

        throw new UnsupportedOperationException("cannot get diff status from" + fnsiOperation + "operation");

    }

    private JsonNode requestDiff(String oid, LocalDateTime fromDate, LocalDateTime toDate, int page){
        DateTimeFormatter dtf =DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return request(Map.of("userKey", userKey,"identifier", oid, "date1", dtf.format(fromDate),  "date2", dtf.format(toDate), "page", page, "size", 200), "/rest/compare");
    }

    private JsonNode requestVersions(String oid) {
        return request(Map.of("userKey", userKey,"identifier", oid, "page", 1, "size", 200), "/rest/versions");
    }

    private JsonNode requestRefBook(String oid) {
        return request(Map.of("userKey", userKey, "identifier", oid, "page", 1, "size", 200),  "/rest/searchDictionary");
    }

    private JsonNode requestStructure(String oid, String version) {
        return request(Map.of("userKey", userKey, "identifier", oid, "version", version), "/rest/passport");

    }

    private JsonNode requestData(String oid, int page, int size) {
        return request(Map.of("userKey", userKey,"identifier", oid,"page", page,"size", size), "/rest/data");
    }

    private JsonNode request(Map<String, Object> queryParams, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fnsiUrl + path);
        for (Map.Entry<String, Object> param : queryParams.entrySet()) {
            builder = builder.queryParam(param.getKey(), param.getValue());
        }
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            String url = builder.toUriString();
            JsonNode body = restTemplate.exchange(
                    new URI(url),
                    HttpMethod.GET,
                    entity,
                    JsonNode.class).getBody();
            if(body == null) {
                throw new FnsiErrorException("response from "+ url + " is empty");
            }
            return body;
        } catch (URISyntaxException e) {
            logger.error("cannot create uri ", e);
            throw new IllegalArgumentException(e);
        }
    }

    private RefBookStructure getRefBookStructure(String code, String lastVersion) {
        JsonNode structureNode = requestStructure(code, lastVersion);
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setPrimaries(
                structureNode.findValues("keys")
                        .stream()
                        .map(jsonnode -> jsonnode.get(0).get("field").asText())
                        .collect(Collectors.toList()));
        Iterator<JsonNode> fields = structureNode.get("fields").elements();
        Map<String, AttributeTypeEnum> attributesAndTypes = new HashMap<>();
        fields.forEachRemaining(jsonNode -> attributesAndTypes.put(jsonNode.get("field").asText(), getAttrType(jsonNode.get("dataType").asText())));
        refBookStructure.setAttributesAndTypes(attributesAndTypes);
        refBookStructure.setReferences(Collections.emptyList());
        return refBookStructure;
    }


    private AttributeTypeEnum getAttrType(String fnsiDataType) {
        switch (fnsiDataType) {
            case "INTEGER":
                return AttributeTypeEnum.INTEGER;
            case "VARCHAR":
                return AttributeTypeEnum.STRING;
            case "DATETIME":
                return AttributeTypeEnum.DATE;
            default:
                return null;
        }
    }
}
