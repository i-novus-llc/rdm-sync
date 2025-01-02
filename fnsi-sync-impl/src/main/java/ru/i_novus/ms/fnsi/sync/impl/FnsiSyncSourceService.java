package ru.i_novus.ms.fnsi.sync.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    private static final String PRIMARY_KEY_TYPE = "PRIMARY";

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
    public RefBookVersion getRefBook(String code, String version) {
        if (version == null) {
            JsonNode response = requestRefBook(code);
            JsonNode listRefBookNodes = response.get("list");
            if (listRefBookNodes.isEmpty()) {
                return null;
            }
            JsonNode refBookNode = listRefBookNodes.get(0);
            RefBookVersion refBook = new RefBookVersion();
            refBook.setFrom(LocalDateTime.parse(refBookNode.get("publishDate").asText(), formatter));
            refBook.setVersion(refBookNode.get("version").asText());
            refBook.setCode(code);
            RefBookStructure refBookStructure = getRefBookStructure(code, refBook.getVersion());
            refBook.setStructure(refBookStructure);
            return refBook;

        } else {
            // Нужны все версии, чтобы заполнить дату закрытия версии
            return getVersions(code).stream()
                    .filter(refBookVersion -> version.equals(refBookVersion.getVersion()))
                    .findAny()
                    .map(refBookVersionItem -> new RefBookVersion(refBookVersionItem, getRefBookStructure(refBookVersionItem.getCode(), refBookVersionItem.getVersion())))
                    .orElse(null);
        }
    }

    @Override
    public List<RefBookVersionItem> getVersions(String code) {
        List<RefBookVersionItem> result = new ArrayList<>();
        int page = 1;
        JsonNode versionsNode = requestVersions(code, page);
        int total = versionsNode.get("total").asInt();
        int pageCount = (total + 200 - 1) /200;
        while (pageCount >= page) {
            if(versionsNode == null) {
                versionsNode = requestVersions(code, page);
            }
            Iterator<JsonNode> versionNodeIterator = versionsNode.get("list").elements();
            while (versionNodeIterator.hasNext()) {
                JsonNode versionNode = versionNodeIterator.next();
                String version = versionNode.get("version").asText();
                result.add(new RefBookVersionItem(code, version, LocalDateTime.parse(versionNode.get("publishDate").asText(), formatter), null, null));
            }
            versionsNode = null;
            page++;
        }
        result.sort(Comparator.comparing(RefBookVersionItem::getFrom));
        for (int i = 0; i < result.size() - 1; i++) {
            if (result.get(i).getTo() == null) {
                result.get(i).setTo(result.get(i + 1).getFrom());
            }
        }
        return result;
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {
        RefBookStructure refBookStructure = dataCriteria.getRefBookStructure();
        JsonNode jsonNode = requestData(dataCriteria.getCode(), dataCriteria.getVersion(), dataCriteria.getPageNumber() + 1, dataCriteria.getPageSize());
        List<Map<String, Object>> data = new ArrayList<>();
        jsonNode.get("list").elements().forEachRemaining(itemNode -> {
            Map<String, Object> row = new LinkedHashMap<>();
            itemNode.elements().forEachRemaining(cellNode -> {
                String value = cellNode.get("value").asText();
                if (!"null".equals(value.trim())) {
                    String column = cellNode.get("column").asText();
                    if (dataCriteria.getFields().contains(column)) {
                        AttributeTypeEnum attributeTypeEnum = refBookStructure.getAttributesAndTypes().get(column);
                        try {
                            row.put(column, attributeTypeEnum.castValue(value));
                        } catch (Exception e) {
                            logger.error("cannot add value = {} to column {}", value, column);
                            throw e;
                        }
                    }
                }
            });
            data.add(row);
        });
        return new PageImpl<>(data, dataCriteria, jsonNode.get("total").asInt());
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {
        if (criteria.getFromDate() == null && criteria.getToDate() == null) {
            initVersionDates(criteria);
        }
        LocalDateTime fromDate = criteria.getFromDate();
        LocalDateTime toDate = criteria.getToDate();
        JsonNode jsonNode = requestDiff(criteria.getRefBookCode(), fromDate, toDate, criteria.getPageNumber() + 1, criteria.getPageSize());
        if ("ERROR".equals(jsonNode.get("result").asText().trim())) {
            return getErrorDiff(jsonNode);
        }
        if (!"null".equals(jsonNode.get("fields").asText().trim())) {
            return VersionsDiff.structureChangedInstance();
        }
        JsonNode diffsNode = jsonNode.get("data").get("list");
        int total = jsonNode.get("data").get("total").intValue();
        List<RowDiff> rowDiffList = new ArrayList<>();
        diffsNode.elements().forEachRemaining(diffNode -> {
            Map<String, AttributeTypeEnum> attributesAndTypes = criteria.getNewVersionStructure().getAttributesAndTypes();
            Map<String, Object> row = new HashMap<>();
            for (Map.Entry<String, AttributeTypeEnum> entry : attributesAndTypes.entrySet()) {
                if(!criteria.getFields().contains(entry.getKey())) {
                    continue;
                }
                JsonNode nodeValue = diffNode.get(entry.getKey());
                if (nodeValue.asText().trim().equals("null")) {
                    continue;
                }
                row.put(entry.getKey(), entry.getValue().castValue(nodeValue.asText()));
            }
            RowDiff rowDiff = new RowDiff(getRowDiffStatusEnum(diffNode.get("operation").asText()), row);
            rowDiffList.add(rowDiff);
        });
        return VersionsDiff.dataChangedInstance(new PageImpl<>(rowDiffList, criteria, total));
    }

    private void initVersionDates(VersionsDiffCriteria criteria) {
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        int page = 1;
        JsonNode versionsNode = requestVersions(criteria.getRefBookCode(), page);
        int totalVersions = versionsNode.get("total").asInt();
        int pageCount = (totalVersions + 200 - 1) /200;
        while ((fromDate == null || toDate == null) && pageCount >= page) {
            if(versionsNode == null) {
                versionsNode = requestVersions(criteria.getRefBookCode(), page);
            }
            Iterator<JsonNode> versionNodeIterator = versionsNode.get("list").elements();
            while (versionNodeIterator.hasNext() && (fromDate == null || toDate == null)) {
                JsonNode versionNode = versionNodeIterator.next();
                if (versionNode.get("version").asText().equals(criteria.getOldVersion())) {
                    fromDate = LocalDateTime.parse(versionNode.get("publishDate").asText(), formatter);
                } else if (versionNode.get("version").asText().equals(criteria.getNewVersion())) {
                    toDate = LocalDateTime.parse(versionNode.get("publishDate").asText(), formatter);
                }
            }
            versionsNode = null;
            page++;

        }
        // кэшируем даты чтобы на последующих страницах не делать лишние запросы за датами
        criteria.setFromDate(fromDate);
        criteria.setToDate(toDate);
    }

    private VersionsDiff getErrorDiff(JsonNode jsonNode) {
        if ("03x0002".equals(jsonNode.get("resultCode").asText().trim())) {
            return VersionsDiff.dataChangedInstance(Page.empty());
        } else {
            throw new FnsiErrorException(jsonNode.get("resultCode").asText("resultText"));
        }
    }

    private RowDiffStatusEnum getRowDiffStatusEnum(String fnsiOperation) {
        switch (fnsiOperation) {
            case "UPDATE" : return RowDiffStatusEnum.UPDATED;
            case "DELETE" : return RowDiffStatusEnum.DELETED;
            case "INSERT" : return RowDiffStatusEnum.INSERTED;
            default : throw new UnsupportedOperationException("cannot get diff status from" + fnsiOperation + "operation");
        }
    }

    private JsonNode requestDiff(String oid, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return request(Map.of("userKey", userKey, "identifier", oid, "date1", dtf.format(fromDate), "date2", dtf.format(toDate), "page", page, "size", size), "/rest/compare");
    }

    private JsonNode requestVersions(String oid, int page) {
        return request(Map.of("userKey", userKey, "identifier", oid, "page", page, "size", 200), "/rest/versions");
    }


    private JsonNode requestRefBook(String oid) {
        return request(Map.of("userKey", userKey, "identifier", oid, "page", 1, "size", 200), "/rest/searchDictionary");
    }

    private JsonNode requestStructure(String oid, String version) {
        return request(Map.of("userKey", userKey, "identifier", oid, "version", version), "/rest/passport");

    }

    private JsonNode requestData(String oid, String version, int page, int size) {
        Map<String, Object> params = Map.of("userKey", this.userKey, "identifier", oid, "page", page, "size", size);
        if (version != null) {
            params = new HashMap<>(params);
            params.put("version", version);
        }
        return request(params, "/rest/data");
    }

    private JsonNode request(Map<String, Object> queryParams, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fnsiUrl + path);
        for (Map.Entry<String, Object> param : queryParams.entrySet()) {
            builder = builder.queryParam(param.getKey(), param.getValue());
        }
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = builder.toUriString();
        try {
            JsonNode body = restTemplate.exchange(
                    new URI(url),
                    HttpMethod.GET,
                    entity,
                    JsonNode.class).getBody();
            if (body == null) {
                throw new FnsiErrorException("response from " + url + " is empty");
            }
            return body;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid uri " + url, e);
        }
    }

    private RefBookStructure getRefBookStructure(String code, String lastVersion) {

        JsonNode structureNode = requestStructure(code, lastVersion);
        final RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setPrimaries(findPrimaries(structureNode));

        final Iterator<JsonNode> fields = structureNode.get("fields").elements();
        Map<String, AttributeTypeEnum> attributesAndTypes = new HashMap<>();
        fields.forEachRemaining(jsonNode ->
                attributesAndTypes.put(jsonNode.get("field").asText(), getAttrType(jsonNode.get("dataType").asText()))
        );
        refBookStructure.setAttributesAndTypes(attributesAndTypes);
        refBookStructure.setReferences(Collections.emptyList());

        return refBookStructure;
    }

    private static List<String> findPrimaries(JsonNode structureNode) {

        final Iterator<JsonNode> keys = structureNode.get("keys").elements();
        List<String> result = new ArrayList<>();
        while (keys.hasNext()) {
            final JsonNode node = keys.next();
            if (node.get("type") != null && PRIMARY_KEY_TYPE.equals(node.get("type").asText())) {
                result.add(node.get("field").asText());
            }
        }
        return result;
    }


    private AttributeTypeEnum getAttrType(String fnsiDataType) {
        switch (fnsiDataType) {
            case "INTEGER":
                return AttributeTypeEnum.INTEGER;
            case "VARCHAR":
                return AttributeTypeEnum.STRING;
            case "DATETIME":
                return AttributeTypeEnum.DATE;
            case "BOOLEAN":
                return AttributeTypeEnum.BOOLEAN;
            default:
                throw new IllegalArgumentException("unknown fnsi type " + fnsiDataType);
        }
    }
}
