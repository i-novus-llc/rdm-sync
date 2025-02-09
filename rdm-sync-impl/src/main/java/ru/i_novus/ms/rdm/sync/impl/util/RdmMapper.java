package ru.i_novus.ms.rdm.sync.impl.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.model.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static ru.i_novus.ms.rdm.sync.api.model.RefBookStructure.Attribute;

@Slf4j
@SuppressWarnings("unchecked")
public class RdmMapper {

    public static final String VERSION_ID_FIELD = "id";

    public static final String DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER = "yyyy-MM-dd'T'HH:mm:ss";

    public static final String DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])T(0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9])$";

    public static final DateTimeFormatter DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER);

    private RdmMapper() {
        // Nothing to do.
    }

    public static RefBookVersion toRefBookVersion(final Map<String, Object> map) {

        if (CollectionUtils.isEmpty(map))
            return null;

        final RefBookVersion result = new RefBookVersion();
        fillRefBookVersionItem(result, map);
        result.setStructure(toRefBookStructure((Map<String, Object>) map.get("structure"), (Map<String, String>) map.get("passport")));

        return result;
    }

    public static List<RefBookVersionItem> toRefBookVersionItems(final Object list) {

        if (!(list instanceof List))
            return emptyList();

        final List<Map<String, Object>> items = (List<Map<String, Object>>) list;
        if (CollectionUtils.isEmpty(items))
            return emptyList();

        return items.stream()
                .map(RdmMapper::toRefBookVersionItem)
                .filter(Objects::nonNull)
                .toList();
    }

    public static RefBookVersionItem toRefBookVersionItem(final Map<String, Object> map) {

        if (CollectionUtils.isEmpty(map))
            return null;

        final RefBookVersionItem result = new RefBookVersionItem();
        fillRefBookVersionItem(result, map);

        return result;
    }

    private static void fillRefBookVersionItem(final RefBookVersionItem item,
                                               final Map<String, Object> map) {

        item.setCode((String) map.get("code"));
        item.setVersion((String) map.get("version"));
        item.setFrom(parseLocalDateTime(map.get("fromDate")));
        item.setTo(parseLocalDateTime(map.get("toDate")));
        item.setVersionId((Integer) map.get(VERSION_ID_FIELD));
    }

    public static RefBookStructure toRefBookStructure(final Map<String, Object> map, Map<String, String> passport) {

        if (CollectionUtils.isEmpty(map))
            return null;

        final List<Map<String, Object>> attributeList = (List<Map<String, Object>>) map.get("attributes");
        final List<String> primaries = attributeList.stream()
                .filter(attribute -> parseBoolean(attribute.get("isPrimary")))
                .map(attribute -> (String) attribute.get("code"))
                .toList();

        final Set<Attribute> attributesAndTypes = attributeList.stream()
                .map(RdmMapper::toAttribute)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final List<Map<String, Object>> referenceList = (List<Map<String, Object>>) map.get("references");
        final List<String> references = CollectionUtils.isEmpty(referenceList)
                ? emptyList()
                : referenceList.stream()
                        .map(reference -> (String) reference.get("referenceCode"))
                        .toList();

        RefBookStructure refBookStructure = new RefBookStructure(references, primaries, attributesAndTypes);
        refBookStructure.setRefDescription(passport != null ? passport.get("name") : null);
        return refBookStructure;
    }

    private static Attribute toAttribute(Map<String, Object> attribute) {

        final AttributeTypeEnum type = toAttributeTypeEnum(attribute.get("type"));
        String description = (String) attribute.get("name");
        return type != null ? new Attribute((String) attribute.get("code"), type, description) : null;
    }

    private static AttributeTypeEnum toAttributeTypeEnum(Object rdmFieldType) {

        return (rdmFieldType instanceof String str) ? AttributeTypeEnum.fromValue(str) : null;
    }

    public static List<Map<String, Object>> toRefBookVersionData(final Object list,
                                                                 final DataCriteria criteria) {

        if (!(list instanceof List))
            return emptyList();

        final List<Map<String, Object>> rows = (List<Map<String, Object>>) list;
        if (CollectionUtils.isEmpty(rows))
            return emptyList();

        final Set<String> fields = criteria.getFields();
        return rows.stream()
                .map(row -> toRefBookVersionDataRow(row, fields))
                .filter(row -> !CollectionUtils.isEmpty(row))
                .toList();
    }

    private static Map<String, Object> toRefBookVersionDataRow(Map<String, Object> row,
                                                               final Set<String> fields) {

        final List<Map<String, Object>> list = (List<Map<String, Object>>) row.get("fieldValues");
        if (CollectionUtils.isEmpty(list))
            return emptyMap();

        final Map<String, Object> items = new LinkedHashMap<>();
        list.stream()
                .filter(item -> fields.contains(toItemFieldName(item)))
                .forEach(item -> items.put(toItemFieldName(item), item.get("value")));

        return items;
    }

    private static String toItemFieldName(Map<String, Object> item) {

        final Object value = item.get("field");
        return (value instanceof String name) ? name : null;
    }

    public static boolean isStructureChanged(Map<String, Object> structureDiff) {

        return !CollectionUtils.isEmpty((List<Map<String, Object>>) structureDiff.get("updated")) ||
                !CollectionUtils.isEmpty((List<Map<String, Object>>) structureDiff.get("deleted")) ||
                !CollectionUtils.isEmpty((List<Map<String, Object>>) structureDiff.get("inserted"));
    }

    public static Map<String, Object> toDataDiffRows(final Object dataDiff) {

        return (dataDiff instanceof Map map) ? (Map<String, Object>) map.get("rows") : emptyMap();
    }

    public static List<RowDiff> toRefBookVersionRowDiff(final Object list,
                                                        final VersionsDiffCriteria criteria) {

        if (!(list instanceof List))
            return emptyList();

        final List<Map<String, Object>> rows = (List<Map<String, Object>>) list;
        if (CollectionUtils.isEmpty(rows))
            return emptyList();

        final Set<String> fields = criteria.getFields();
        return rows.stream()
                .map(row -> toRefBookVersionRowDiff(row, fields))
                .filter(Objects::nonNull)
                .toList();
    }

    public static RowDiff toRefBookVersionRowDiff(final Map<String, Object> row,
                                                  final Set<String> fields) {

        final List<Map<String, Object>> list = (List<Map<String, Object>>) row.get("values");
        if (CollectionUtils.isEmpty(list))
            return null;

        final RowDiffStatusEnum status = RowDiffStatusEnum.fromValue((String) row.get("status"));

        final Map<String, Object> items = new LinkedHashMap<>();
        list.stream()
                .filter(item -> fields.contains(toDiffItemFieldName(item)))
                .forEach(item -> {
                    if (status.equals(RowDiffStatusEnum.DELETED)) {
                        items.put(toDiffItemFieldName(item), item.get("oldValue"));
                    } else {
                        items.put(toDiffItemFieldName(item), item.get("newValue"));
                    }
                });

        return new RowDiff(status, items);
    }

    private static String toDiffItemFieldName(Map<String, Object> item) {

        final Object value = item.get("field");
        return (value instanceof Map map) ? (String) map.get("name") : null;
    }

    public static boolean parseBoolean(Object value) {

        if (value == null)
            return false;

        if (value instanceof Boolean bool)
            return Boolean.TRUE.equals(bool);

        if (value instanceof String str)
            return Boolean.parseBoolean(str);

        return false;
    }

    public static Long parseLong(Object value) {

        if (value == null)
            return null;

        if (value instanceof Long longValue)
            return longValue;

        if (value instanceof Integer intValue)
            return intValue.longValue();

        if (!(value instanceof String str))
            return null;

        return !StringUtils.isEmpty(str) ? Long.parseLong(str) : null;
    }

    public static LocalDateTime parseLocalDateTime(Object value) {

        if (value == null)
            return null;

        if (!(value instanceof String str))
            return null;

        if (StringUtils.isEmpty(str))
            return null;

        try {
            if (str.matches(DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_FORMATTER);
            }

            throw new IllegalArgumentException("Failed to parse Date&Time: " + str);

        } catch (DateTimeException e) {
            log.debug("Failed to parse Date&Time " + str, e);
            throw new IllegalArgumentException("Failed to parse Date&Time: " + str);
        }
    }
}
