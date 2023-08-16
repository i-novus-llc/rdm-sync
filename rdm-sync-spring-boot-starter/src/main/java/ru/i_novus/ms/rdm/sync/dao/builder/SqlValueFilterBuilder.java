package ru.i_novus.ms.rdm.sync.dao.builder;

import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.replace;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addDoubleQuotes;

/**
 * Построитель условий по фильтру значений поля.
 *
 */
public class SqlValueFilterBuilder extends SqlClauseBuilder {

    private static final String OPERATOR_LIKE = "LIKE";
    private static final String OPERATOR_ILIKE = "ILIKE";

    public SqlValueFilterBuilder() {
    }

    public SqlValueFilterBuilder(List<String> clauses, Map<String, Serializable> params) {
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
            case EQUAL : parseEqual(fieldName, bindName, filter.getValues());
            case LIKE : parseLike(fieldName, type, bindName, filter.getValues());
            case ILIKE : parseILike(fieldName, type, bindName, filter.getValues());
            case QLIKE : parseQLike(fieldName, type, bindName, filter.getValues());
            case IQLIKE : parseIQLike(fieldName, type, bindName, filter.getValues());
            case IS_NULL : parseIsNull(fieldName);
            case IS_NOT_NULL : parseIsNotNull(fieldName);
            default : throw new RdmException("Unknown type '" + filter.getType() + "' of field value filter");
        }
    }

    private void parseEqual(String field, String bindName, List<? extends Serializable> values) {

        if (values.size() == 1) {
            append(field + " = :" + bindName);
            bind(bindName, values.get(0));

        } else {
            append(field + " IN (:" + bindName + ")");
            bind(bindName, new ArrayList<>(values));
        }
    }

    private void parseLike(String field, DataTypeEnum type, String bindName, List<? extends Serializable> values) {

        parseLike(getStringFieldSubst(field, type), bindName, OPERATOR_LIKE, values);
    }

    private void parseILike(String field, DataTypeEnum type, String bindName, List<? extends Serializable> values) {

        parseLike(getStringFieldSubst(field, type), bindName, OPERATOR_ILIKE, values);
    }

    private void parseQLike(String field, DataTypeEnum type, String bindName, List<? extends Serializable> values) {

        parseLike(getQuotesIgnoredFieldSubst(field, type), bindName, OPERATOR_LIKE, values);
    }

    private void parseIQLike(String field, DataTypeEnum type, String bindName, List<? extends Serializable> values) {

        parseLike(getQuotesIgnoredFieldSubst(field, type), bindName, OPERATOR_ILIKE, values);
    }

    private void parseLike(String fieldSubst, String bindName, String operator,
                           List<? extends Serializable> values) {

        IntStream.range(0, values.size()).forEach(index -> {

            Serializable value = values.get(index);
            String indexName = bindName + "_" + index;
            String valueName = (value instanceof String) ? indexName : indexName + "::text";

            append(fieldSubst + " " + operator + " '%' || :" + valueName + " || '%'");
            bind(indexName, values.get(index));
        });
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

        return (type == DataTypeEnum.VARCHAR) ? field : field + "::text";
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
