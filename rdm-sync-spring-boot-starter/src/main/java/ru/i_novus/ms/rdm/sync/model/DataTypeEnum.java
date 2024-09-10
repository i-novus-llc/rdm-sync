package ru.i_novus.ms.rdm.sync.model;

import org.apache.commons.lang3.StringUtils;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Тип поля с соответствующими ему типами данных в БД.
 */
public enum DataTypeEnum {

    VARCHAR(asList("varchar", "text", "character varying"), String.class),
    INTEGER(asList("integer", "smallint", "bigint", "serial", "bigserial"), BigInteger.class),
    INTEGER_ARRAY(singletonList("integer[]"), Integer[].class),
    STRING_ARRAY(singletonList("text[]"), String[].class),
    DATE(singletonList("date"), LocalDate.class),
    BOOLEAN(singletonList("boolean"), Boolean.class),
    FLOAT(asList("decimal", "numeric"), BigDecimal.class),
    JSONB(singletonList("jsonb"), Object.class);

    private static final Map<String, DataTypeEnum> DATA_TYPE_MAP = new HashMap<>();
    static {
        for (DataTypeEnum type : DataTypeEnum.values()) {
            for (String dataType : type.dataTypes) {
                DATA_TYPE_MAP.put(dataType, type);
            }
        }
    }

    private final List<String> dataTypes;
    private final Class<?> clazz;

    DataTypeEnum(List<String> dataTypes, Class<?> clazz) {
        this.dataTypes = dataTypes;
        this.clazz = clazz;
    }

    public List<String> getDataTypes() {
        return dataTypes;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public List<Serializable> toValues(List<String> list) {
        switch (this) {
            case BOOLEAN:
                return list.stream().map(Boolean::valueOf).collect(toList());
            case INTEGER:
                return list.stream().map(BigInteger::new).collect(toList());
            case FLOAT:
                return list.stream().map(BigDecimal::new).collect(toList());
            case DATE:
                return list.stream().map(TimeUtils::parseLocalDate).collect(toList());
            case VARCHAR:
                return new ArrayList<>(list);
            default:
                throw new RdmException("Cast from string to " + this.name() + " not supported.");
        }
    }

    public Serializable toValue(String s) {
        return toValues(singletonList(s)).get(0);
    }

    public Map<FilterTypeEnum, List<Object>> toMap(List<String> list) {
        Map<FilterTypeEnum, List<Object>> result = new HashMap<>(FilterTypeEnum.size());
        FilterTypeEnum.list().forEach(type -> result.put(type, new ArrayList<>(list.size())));

        switch (this) {
            case BOOLEAN:
                addListValues(list, Boolean::valueOf, result);
                break;
            case INTEGER:
                addListValues(list, BigInteger::new, result);
                break;
            case FLOAT:
                addListValues(list, BigDecimal::new, result);
                break;
            case DATE:
                addListValues(list, TimeUtils::parseLocalDate, result);
                break;
            case VARCHAR:
                addListValues(list, this::fromString, result);
                break;
            case STRING_ARRAY:
                addListValues(list, rawString -> rawString.trim().split(";"), result);
                break;
            case INTEGER_ARRAY:
                addListValues(list, rawString -> Arrays.stream(rawString.trim().split(";")).map(Integer::valueOf).collect(toList()).toArray(new Integer[0]), result);
                break;
            default:
                throw new RdmException("Cast from string to " + this.name() + " not supported.");
        }

        return result;
    }

    private Serializable fromString(String value) {
        return StringUtils.isEmpty(value) ? null : value;
    }

    private void addListValues(List<String> list, Function<String, Object> converter, Map<FilterTypeEnum, List<Object>> result) {
        list.stream()
                .filter(item -> !StringUtils.isEmpty(item))
                .forEach(item -> addItemValue(item, converter, result));
    }

    private void addItemValue(String item, Function<String, Object> converter, Map<FilterTypeEnum, List<Object>> result) {
        int index = (item.indexOf("$") == 0) ? item.indexOf("|", 1) : -1;
        String mask = (index > 0) ? item.substring(1, index) : FilterTypeEnum.EQUAL.getMask();
        FilterTypeEnum filterType = FilterTypeEnum.fromMask(mask);
        if (filterType == null) return;

        Object filterValue = null;
        if (filterType.isValued()) {
            String value = (index > 0) ? item.substring(index + 1) : item;
            filterValue = converter.apply(value);
            if (filterValue == null) return;
        }

        result.get(filterType).add(filterValue);
    }

    public static DataTypeEnum getByDataType(String dataType) {
        return dataType == null ? null : DATA_TYPE_MAP.get(dataType);
    }

    public static DataTypeEnum getByRdmAttr(AttributeTypeEnum type) {
        switch (type) {
            case DATE:
                return DATE;
            case FLOAT:
                return FLOAT;
            case INTEGER:
                return INTEGER;
            case STRING:
                return VARCHAR;
            case REFERENCE:
                return VARCHAR;
            case BOOLEAN:
                return BOOLEAN;
            default:
                throw new RdmException(String.format("Attribute type '%s' is not supported", type));
        }
    }
}
