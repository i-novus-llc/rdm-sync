package ru.i_novus.ms.rdm.sync.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public enum AttributeTypeEnum {

    STRING(String::toString),
    INTEGER(Integer::valueOf),
    FLOAT(Float::valueOf),
    DATE(AttributeTypeEnum::toLocalDate),
    BOOLEAN(Boolean::valueOf),
    REFERENCE(String::toString),
    TREE(String::toString),
    INT_ARRAY(toIntArray()),
    STRING_ARRAY(toStringArray());

    private static final ZoneId LOCAL_DATE_ZONE_ID = ZoneId.of("Europe/Moscow");
    private static final int MILLIS_DATE_LENGTH = 13;
    private static final String DATE_ITEM_EUROPEAN_SEPARATOR = ".";
    private static final String DATE_EUROPEAN_PATTERN = "dd.MM.yyyy";
    private static final String ITEM_SEPARATOR_REGEX = ";";

    private static final Map<String, AttributeTypeEnum> TYPE_MAP = new HashMap<>();
    static {
        for (AttributeTypeEnum type : AttributeTypeEnum.values()) {
            TYPE_MAP.put(type.name(), type);
        }
    }

    private  final Function<String, Object> castFunction;

    AttributeTypeEnum(Function<String, Object> castFunction) {
        this.castFunction = castFunction;
    }

    public Object castValue(String value) {
        try {
            return castFunction.apply(value);

        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot cast value='" + value + "' to type " + this.name());
        }
    }

    /**
     * Получение типа по строковому значению типа.
     * <p/>
     * Значение null может (но не должно) прийти из RDM.
     * Обычный {@link AttributeTypeEnum#valueOf} не подходит, т.к. кидает исключение.
     *
     * @param value Строковое значение
     * @return Тип атрибута
     */
    public static AttributeTypeEnum fromValue(String value) {

        return value != null ? TYPE_MAP.get(value) : null;
    }

    private static LocalDate toLocalDate(String value) {

        if (value.length() == MILLIS_DATE_LENGTH) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(value)), LOCAL_DATE_ZONE_ID);
        }

        if (value.contains(DATE_ITEM_EUROPEAN_SEPARATOR)) {
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_EUROPEAN_PATTERN);
            return LocalDate.parse(value, dateTimeFormatter);

        } else {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    private static Function<String, Object> toIntArray() {

        return value -> Arrays.stream(value.trim().split(ITEM_SEPARATOR_REGEX))
                .map(item -> Integer.valueOf(item.trim()))
                .collect(toList());
    }

    private static Function<String, Object> toStringArray() {
        return value -> Arrays.stream(value.trim().split(ITEM_SEPARATOR_REGEX))
                .map(item -> item.trim())
                .collect(toList());
    }

}
