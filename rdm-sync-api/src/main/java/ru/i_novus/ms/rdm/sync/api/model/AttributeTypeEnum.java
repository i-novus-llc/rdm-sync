package ru.i_novus.ms.rdm.sync.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public enum AttributeTypeEnum {
    STRING(String::toString),
    INTEGER(Integer::valueOf),
    FLOAT(Float::valueOf),
    DATE(value -> {
        if(value.length() == 13) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(Long.valueOf(value)),
                    ZoneId.of("Europe/Moscow"));

        }
        if (value.contains(".")) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return LocalDate.parse(value, dateTimeFormatter);
        } else {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }),
    BOOLEAN(Boolean::valueOf),
    REFERENCE(String::toString),
    TREE(String::toString);

    private  final Function<String, Object> castFunction;

    AttributeTypeEnum(Function<String, Object> castFunction) {
        this.castFunction = castFunction;
    }

    public Object castValue(String value) {
        try {
            return castFunction.apply(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot cast value=" + value + " to type " + this.name());
        }
    }
}
