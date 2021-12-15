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
import static ru.i_novus.ms.rdm.api.util.StringUtils.addDoubleQuotes;

/**
 * Построитель условий по фильтру значений поля.
 *
 */
public class SqlValueFilterBuilder extends SqlClauseBuilder {

    public SqlValueFilterBuilder() {
    }

    public SqlValueFilterBuilder(List<String> clauses, Map<String, Serializable> params) {
        super(clauses, params);
    }

    @Override
    public String build() {
        return collect(joining("\n OR "));
    }

    public void parse(String field, DataTypeEnum type, String name, FieldValueFilter filter) {
        
        if (isEmpty(field) || isEmpty(name) || filter == null || filter.isEmpty())
            return;

        String fieldName = addDoubleQuotes(field);
        switch (filter.getType()) {
            case EQUAL -> parseEqual(fieldName, name, filter.getValues());
            case LIKE -> parseLike(fieldName, type, name, filter.getValues());
            default -> throw new RdmException("Unknown type '" + filter.getType() + "' of field value filter");
        };
    }

    private void parseEqual(String field, String name, List<? extends Serializable> values) {

        if (values.size() == 1) {
            append(field + " = :" + name);
            bind(name, values.get(0));

        } else {
            append(field + " IN (:" + name + ")");
            bind(name, new ArrayList<>(values));
        }
    }

    private void parseLike(String field, DataTypeEnum type, String name, List<? extends Serializable> values) {

        String fieldName = (type == DataTypeEnum.VARCHAR) ? field : field + "::text";

        IntStream.range(0, values.size()).forEach(index -> {

            Serializable value = values.get(index);
            String bindName = name + "_" + index;
            String bindText = (value instanceof String) ? bindName : bindName + "::text";

            append(fieldName + " LIKE '%' || :" + bindText + " || '%'");
            bind(bindName, values.get(index));
        });
    }
}
