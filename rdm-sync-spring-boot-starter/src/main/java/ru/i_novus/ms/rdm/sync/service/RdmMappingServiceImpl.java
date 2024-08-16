package ru.i_novus.ms.rdm.sync.service;

import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public class RdmMappingServiceImpl implements RdmMappingService {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final DateTimeFormatter EU_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public Object map(AttributeTypeEnum attributeType, FieldMapping fieldMapping, Object value) {
        DataTypeEnum clientType = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());
        String transformExpr = fieldMapping.getTransformExpression();
        if (value == null || "".equals(value)) {
            return AttributeTypeEnum.BOOLEAN.equals(attributeType) ? mapBoolean(clientType, value) : null;
        }

        if(attributeType == null) {
            attributeType = getAttributeType(clientType);
        }

        if (transformExpr != null) {
            Class<?> type = getAttributeType(clientType).getDeclaringClass().getDeclaringClass();
            return evaluateExpression(transformExpr, value, type);
        }

        Object result = null;
        switch (attributeType) {
            case STRING:
                return mapVarchar(clientType, value);
            case INTEGER:
                return mapInteger(clientType, value);
            case BOOLEAN:
                return mapBoolean(clientType, value);
            case FLOAT:
                return mapFloat(clientType, value);
            case DATE:
                return mapDate(clientType, value);
            case TREE:
                return value.toString();
            case REFERENCE:
                return mapReference(clientType, value);
            case INT_ARRAY:
                return mapIntegerArray(value);
            case STRING_ARRAY:
                return mapTextArray(value);
        }

        return result;
    }
    private List<Integer> mapIntegerArray(Object value) {
        return (List<Integer>)value;
    }

    private List<String> mapTextArray(Object value) {
        return (List<String>)value;
    }

    private AttributeTypeEnum getAttributeType(DataTypeEnum clientType) {
        switch (clientType) {
            case BOOLEAN:
                return AttributeTypeEnum.BOOLEAN;
            case DATE:
                return AttributeTypeEnum.DATE;
            case FLOAT:
                return AttributeTypeEnum.FLOAT;
            case INTEGER:
                return AttributeTypeEnum.INTEGER;
            case JSONB:
                return AttributeTypeEnum.REFERENCE;
            case VARCHAR:
                return AttributeTypeEnum.STRING;

        }
        throw new IllegalArgumentException("unknown type " + clientType);
    }

    private Object mapInteger(DataTypeEnum clientType, Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }

        switch (clientType) {
            case INTEGER:
                return new BigInteger(value.toString());
            case VARCHAR:
                return value.toString();
            case FLOAT:
                return Float.parseFloat(value.toString());
            default:
                throw new ClassCastException(getClassCastError(FieldType.INTEGER, clientType, value));
        }
    }

    private Object mapVarchar(DataTypeEnum clientType, Object value) {

        if (value == null || "".equals(value)) {
            return null; // Преобразуем пустую строку в null
        }

        String valueStr = value.toString();
        switch (clientType) {
            case VARCHAR:
                return value;
            case INTEGER:
                return new BigInteger(valueStr);
            case FLOAT:
                return Float.parseFloat(valueStr);
            case BOOLEAN:
                return Boolean.parseBoolean(valueStr);
            case DATE: {
                DateTimeFormatter dateTimeFormatter;
                if(valueStr.contains(".")) {
                    dateTimeFormatter = EU_DATE_FORMATTER;
                } else {
                    dateTimeFormatter = ISO_DATE_FORMATTER;
                }
                return LocalDate.parse(valueStr, dateTimeFormatter);
            }
            default:
                throw new ClassCastException(getClassCastError(FieldType.STRING, clientType, value));
        }
    }

    private Object mapDate(DataTypeEnum clientType, Object value) {
        if (clientType.equals(DataTypeEnum.DATE)) {
            if (value instanceof LocalDate)
                return value;
            else if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            } else if (value instanceof Date) {
                return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                return LocalDate.parse(value.toString(), ISO_DATE_FORMATTER);
            }
        } else if (clientType.equals(DataTypeEnum.VARCHAR)) {
            if (value instanceof Date) {
                return FastDateFormat.getInstance(DATE_FORMAT).format(value);
            } else if (value instanceof LocalDate || value instanceof LocalDateTime) {
                return ISO_DATE_FORMATTER.format((Temporal) value);
            } else {
                throw new ClassCastException(getClassCastError(FieldType.DATE, clientType, value));
            }
        } else {
            throw new ClassCastException(getClassCastError(FieldType.DATE, clientType, value));
        }
    }

    private Object mapBoolean(DataTypeEnum clientType, Object value) {
        if (value == null)
            value = "false";
        if (clientType.equals(DataTypeEnum.VARCHAR)) {
            return value.toString();
        } else if (clientType.equals(DataTypeEnum.BOOLEAN)) {
            return Boolean.parseBoolean(value.toString());
        } else {
            throw new ClassCastException(getClassCastError(FieldType.BOOLEAN, clientType, value));
        }
    }

    private Object mapFloat(DataTypeEnum clientType, Object value) {
        if (clientType.equals(DataTypeEnum.VARCHAR)) {
            return value.toString();
        } else if (clientType.equals(DataTypeEnum.FLOAT)) {
            return Float.parseFloat(value.toString());
        } else {
            throw new ClassCastException(getClassCastError(FieldType.FLOAT, clientType, value));
        }
    }

    private Object mapReference(DataTypeEnum clientType, Object value) {
        String refValue;
        Reference reference = null;
        if (value instanceof Reference) {
            reference = (Reference) value;
            refValue = reference.getValue();
        } else {
            if (value == null)
                return null;
            refValue = value.toString();
        }
        switch (clientType) {
            case VARCHAR:
                return refValue;
            case INTEGER:
                return new BigInteger(refValue);
            case FLOAT:
                return Float.parseFloat(refValue);
            case BOOLEAN:
                return Boolean.parseBoolean(refValue);
            case DATE:
                return LocalDate.parse(refValue, ISO_DATE_FORMATTER);
            case JSONB:
                return reference == null ? refValue : reference;
            default:
                throw new ClassCastException(getClassCastError(FieldType.REFERENCE, clientType, value));
        }
    }

    private String getClassCastError(FieldType rdmType, DataTypeEnum clientType, Object value) {
        return String.format("Error while casting %s to %s. Value: %s", rdmType, clientType, value);
    }

    private Object evaluateExpression(String expr, Object input, Class<?> returnType) {

        // Создание контекста для оценки SpEL выражения
        SimpleEvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withRootObject(input)
                .build();

        ExpressionParser parser = new SpelExpressionParser();

        return parser.parseExpression(expr).getValue(context, returnType);
    }

}
