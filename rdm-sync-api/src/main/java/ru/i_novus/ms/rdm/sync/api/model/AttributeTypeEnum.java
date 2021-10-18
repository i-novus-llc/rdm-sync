package ru.i_novus.ms.rdm.sync.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
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
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return LocalDate.parse(value, dateTimeFormatter);
    }),
    BOOLEAN(Boolean::valueOf),
    REFERENCE(String::toString),
    TREE(String::toString);

    private Function<String, Object> castFunction;

    AttributeTypeEnum(Function<String, Object> castFunction) {
        this.castFunction = castFunction;
    }

    public Object castValue(String value) {
        return castFunction.apply(value);
    }
}
