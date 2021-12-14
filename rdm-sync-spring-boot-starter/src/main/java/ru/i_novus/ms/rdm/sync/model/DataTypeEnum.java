package ru.i_novus.ms.rdm.sync.model;

import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public enum DataTypeEnum {

    VARCHAR(asList("varchar", "text", "character varying")),
    INTEGER(asList("integer", "smallint", "bigint", "serial", "bigserial")),
    DATE(singletonList("date")),
    BOOLEAN(singletonList("boolean")),
    FLOAT(asList("decimal", "numeric")),
    JSONB(singletonList("jsonb"));

    private static final Map<String, DataTypeEnum> TYPE_MAP = new HashMap<>();
    static {
        for (DataTypeEnum dt : DataTypeEnum.values()) {
            for (String s : dt.dataTypes)
                TYPE_MAP.put(s, dt);
        }
    }

    private final List<String> dataTypes;

    DataTypeEnum(List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public List<String> getDataTypes() {
        return dataTypes;
    }

    @SuppressWarnings("squid:S1452")
    public List<Serializable> castFromString(List<String> list) {

        return switch (this) {
            case BOOLEAN -> list.stream().map(Boolean::valueOf).collect(toList());
            case INTEGER -> list.stream().map(BigInteger::new).collect(toList());
            case FLOAT -> list.stream().map(BigDecimal::new).collect(toList());
            case DATE -> list.stream().map(TimeUtils::parseLocalDate).collect(toList());
            case VARCHAR -> new ArrayList<>(list);
            default -> throw new RdmException("Cast from string to " + this.name() + " not supported.");
        };
    }

    public Serializable castFromString(String s) {
        return castFromString(singletonList(s)).get(0);
    }

    public static DataTypeEnum getByDataType(String dataType) {

        return dataType == null ? null : TYPE_MAP.get(dataType);
    }

    public static DataTypeEnum getByRdmAttr(AttributeTypeEnum type) {


        return switch (type) {
            case DATE -> DATE;
            case FLOAT -> FLOAT;
            case INTEGER -> INTEGER;
            case STRING, REFERENCE -> VARCHAR;
            case BOOLEAN -> BOOLEAN;
            default -> throw new RdmException(String.format("Attribute type '%s' is not supported", type));
        };
    }
}

