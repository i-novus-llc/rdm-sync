package ru.i_novus.ms.rdm.sync.init.mapping;

import org.apache.commons.lang3.StringUtils;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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



    private Serializable fromString(String value) {
        return StringUtils.isEmpty(value) ? null : value;
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
                throw new RdmSyncException(String.format("Attribute type '%s' is not supported", type));
        }
    }
}
