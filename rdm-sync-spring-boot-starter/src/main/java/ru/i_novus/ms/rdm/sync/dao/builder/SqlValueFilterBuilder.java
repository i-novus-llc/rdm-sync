package ru.i_novus.ms.rdm.sync.dao.builder;

import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.replace;
import static ru.i_novus.ms.rdm.sync.util.StringUtils.addDoubleQuotes;

/**
 * Построитель условий по фильтру значений поля.
 *
 */
public class SqlValueFilterBuilder extends SqlClauseBuilder {

    private static final String OPERATOR_LIKE = "LIKE";
    private static final String OPERATOR_ILIKE = "ILIKE";

    public SqlValueFilterBuilder() {
    }

    public SqlValueFilterBuilder(List<String> clauses, Map<String, Object> params) {
        super(clauses, params);
    }

    @Override
    public String build() {
        return collect(joining("\n OR "));
    }

    public void parse(String field, DataTypeEnum type, String bindName, FieldValueFilter filter) {
        
        if (isEmpty(field) || isEmpty(bindName) || filter == null || filter.isEmpty())
            return;

        String fieldName = addDoubleQuotes(field);
        switch (filter.getType()) {
            case EQUAL:
                parseEqual(fieldName, bindName, filter.getValues());
                break;
            case LIKE:
                parseLike(fieldName, type, bindName, filter.getValues());
                break;
            case ILIKE:
                parseILike(fieldName, type, bindName, filter.getValues());
                break;
            case QLIKE:
                parseQLike(fieldName, type, bindName, filter.getValues());
                break;
            case IQLIKE:
                parseIQLike(fieldName, type, bindName, filter.getValues());
                break;
            case IS_NULL:
                parseIsNull(fieldName);
                break;
            case IS_NOT_NULL:
                parseIsNotNull(fieldName);
                break;
            default:
                throw new RdmSyncException("Unknown type '" + filter.getType() + "' of field value filter");
        }
    }

    private void parseEqual(String field, String bindName, List<Object> values) {

        List<Object> preparedValues = values.stream().map(value -> {
            if(value == null)
                return value;
            if(value instanceof String[]) {
                final String[] valueArr = (String[])value;
                return new AbstractSqlTypeValue() {
                    @Override
                    protected Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException {
                        return con.createArrayOf("text", valueArr);
                    }
                };
            } else if (value instanceof Integer[]) {
                final Integer[] valueArr = (Integer[]) value;
                return new AbstractSqlTypeValue() {
                    @Override
                    protected Object createTypeValue(Connection con, int sqlType, String typeName) throws SQLException {
                        return con.createArrayOf("integer", valueArr);
                    }
                };
            }
            return value;
        }).collect(Collectors.toList());

        if (preparedValues.size() == 1) {
            append(field + " = :" + bindName);
            bind(bindName, preparedValues.get(0));

        } else {
            append(field + " IN (:" + bindName + ")");
            bind(bindName, new ArrayList<>(preparedValues));
        }
    }

    private void parseLike(String field, DataTypeEnum type, String bindName, List<Object> values) {

        parseLike(getStringFieldSubst(field, type), bindName, OPERATOR_LIKE, values);
    }

    private void parseILike(String field, DataTypeEnum type, String bindName, List<Object> values) {

        parseLike(getStringFieldSubst(field, type), bindName, OPERATOR_ILIKE, values);
    }

    private void parseQLike(String field, DataTypeEnum type, String bindName, List<Object> values) {

        parseLike(getQuotesIgnoredFieldSubst(field, type), bindName, OPERATOR_LIKE, values);
    }

    private void parseIQLike(String field, DataTypeEnum type, String bindName, List<Object> values) {

        parseLike(getQuotesIgnoredFieldSubst(field, type), bindName, OPERATOR_ILIKE, values);
    }

    private void parseLike(String fieldSubst, String bindName, String operator,
                           List<Object> values) {

        List<Object> preparedValues = tryArrayToString(values);

        IntStream.range(0, preparedValues.size()).forEach(index -> {

            Object value = preparedValues.get(index);
            String indexName = bindName + "_" + index;
            String valueName = (value instanceof String) ? indexName : indexName + "::text";

            append(fieldSubst + " " + operator + " '%' || :" + valueName + " || '%'");
            bind(indexName, preparedValues.get(index));
        });
    }

    private List<Object> tryArrayToString(List<Object> values) {
        return values.stream().map(value -> {
            if(value instanceof String[] || value instanceof Integer[]) {
                return ((Object[]) value)[0].toString();
            }
            return value;
        }).collect(Collectors.toList());
    }

    private void parseIsNull(String field) {

        parseIs(field, "NULL");
    }

    private void parseIsNotNull(String field) {

        parseIs(field, "NOT NULL");
    }

    private void parseIs(String field, String value) {

        append(field + " IS " + value);
    }

    /* Подстановка для строкового представления поля. */
    private String getStringFieldSubst(String field, DataTypeEnum type) {
        switch (type) {
            case VARCHAR:
                return field;
            case STRING_ARRAY:
                return "array_to_string(" + field + ", ',')";
            default:
                return field + "::text";
        }
    }

    /* Строковое представление для поля. */
    private String getQuotesIgnoredFieldSubst(String field, DataTypeEnum type) {

        return (type == DataTypeEnum.VARCHAR) ? getCharsIgnoredSubst(field, "'\"") : field + "::text";
    }

    /* Подстановка строкового поля для игнорированием символов. */
    private String getCharsIgnoredSubst(String field, String chars) {

        if (isEmpty(chars))
            return field;

        chars = replace(chars, "'", "''"); // Escape single quote!
        return "translate(" + field + ", '" + chars + "', '')";
    }
}
