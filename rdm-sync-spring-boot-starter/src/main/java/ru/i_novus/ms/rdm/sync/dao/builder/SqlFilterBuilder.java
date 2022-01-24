package ru.i_novus.ms.rdm.sync.dao.builder;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;
import ru.i_novus.ms.rdm.sync.model.filter.FieldValueFilter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Построитель условий по фильтру поля.
 */
public class SqlFilterBuilder extends SqlClauseBuilder {

    public SqlFilterBuilder() {
    }

    public SqlFilterBuilder(List<String> clauses, Map<String, Serializable> params) {
        super(clauses, params);
    }

    @Override
    public String build() {
        return collect(joining("\n AND "));
    }

    public void parse(FieldFilter filter) {
        
        String field = filter.getField();
        List<FieldValueFilter> valueFilters = filter.getFilters();
        if (CollectionUtils.isEmpty(valueFilters))
            return;

        SqlValueFilterBuilder builder = new SqlValueFilterBuilder();
        IntStream.range(0, valueFilters.size()).forEach(index -> {
            final String bindName = field + "_" + index;
            builder.parse(field, filter.getType(), bindName, valueFilters.get(index));
        });
        concat(builder);
    }

    @Override
    public void concat(ClauseBuilder builder) {

        if (builder == null)
            return;

        String clause = builder.build();
        if (!StringUtils.isEmpty(clause)) {
            clause = clause.contains("\n") ? "(\n" + clause + "\n)" : clause;
        }

        concat(clause, builder.getParams());
    }
}
