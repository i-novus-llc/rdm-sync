package ru.i_novus.ms.rdm.sync.model;

import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FilterTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Тип поля с соответствующими ему типами данных в БД.
 */
public enum DataTypeEnum {

    VARCHAR(asList("varchar", "text", "character varying")),
    INTEGER(asList("integer", "smallint", "bigint", "serial", "bigserial")),
    INTEGER_ARRAY(singletonList("integer[]")),
    STRING_ARRAY(singletonList("text[]")),
    DATE(singletonList("date")),
    BOOLEAN(singletonList("boolean")),
    FLOAT(asList("decimal", "numeric")),
    JSONB(singletonList("jsonb"));

    private static final Map<String, DataTypeEnum> DATA_TYPE_MAP = new HashMap<>();

    static {
        for (DataTypeEnum type : DataTypeEnum.values()) {
            for (String dataType : type.dataTypes)
                DATA_TYPE_MAP.put(dataType, type);
        }
    }

    private final List<String> dataTypes;

    DataTypeEnum(List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public List<String> getDataTypes() {
        return dataTypes;
    }

    /** Преобразование списка строк в список значений соответствующего типа. */
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

    /** Преобразование строки в значение соответствующего типа. */
    public Serializable toValue(String s) {
        return toValues(singletonList(s)).get(0);
    }

    /** Преобразование списка строк в набор значений с соответствующими фильтрами. */
    public Map<FilterTypeEnum, List<Object>> toMap(List<String> list) {

        Map<FilterTypeEnum, List<Object>> result = new HashMap<>(FilterTypeEnum.size());
        FilterTypeEnum.list().forEach(type -> result.put(type, new ArrayList<>(list.size())));

        switch (this) {
            case BOOLEAN:
                addListValues(list, Boolean::valueOf, result);
                return result;
            case INTEGER:
                addListValues(list, BigInteger::new, result);
                return result;
            case FLOAT:
                addListValues(list, BigDecimal::new, result);
                return result;
            case DATE:
                addListValues(list, TimeUtils::parseLocalDate, result);
                return result;
            case VARCHAR:
                addListValues(list, this::fromString, result);
                return result;
            case STRING_ARRAY:
                addListValues(list, rawString -> rawString.trim().split(";"), result);
                return result;
            case INTEGER_ARRAY:
                addListValues(list, rawString -> Arrays.stream(rawString.trim().split(";")).map(Integer::valueOf).collect(toList()).toArray(new Integer[0]), result);
                return result;
            default:
                throw new RdmException("Cast from string to " + this.name() + " not supported.");
        }


    }

    /** Конвертер для значений-строк. */
    private Serializable fromString(String value) {
        return StringUtils.isEmpty(value) ? null : value;
    }

    /** Преобразование списка строк в значения и добавление этих значений в набор. */
    private void addListValues(List<String> list,
                               Function<String, Object> converter,
                               Map<FilterTypeEnum, List<Object>> result) {

        list.stream()
                .filter(item -> !StringUtils.isEmpty(item))
                .forEach(item -> addItemValue(item, converter, result));
    }

    /** Преобразование строки в значение и добавление этого значения в набор. */
    private void addItemValue(String item,
                              Function<String, Object> converter,
                              Map<FilterTypeEnum, List<Object>> result) {

        int index = (item.indexOf("$") == 0) ? item.indexOf("|", 1) : -1;
        String mask = (index > 0) ? item.substring(1, index) : FilterTypeEnum.EQUAL.getMask();
        FilterTypeEnum filterType = FilterTypeEnum.fromMask(mask);
        if (filterType == null)
            return;

        Object filterValue = null;
        if (filterType.isValued()) {
            String value = (index > 0) ? item.substring(index + 1) : item;
            filterValue = converter.apply(value);
            if (filterValue == null)
                return;
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

