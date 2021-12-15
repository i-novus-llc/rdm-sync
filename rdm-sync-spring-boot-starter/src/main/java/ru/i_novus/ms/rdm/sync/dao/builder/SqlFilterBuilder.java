package ru.i_novus.ms.rdm.sync.dao.builder;

import org.springframework.util.CollectionUtils;
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

        IntStream.range(0, valueFilters.size()).forEach(index -> {

            SqlValueFilterBuilder builder = new SqlValueFilterBuilder();
            builder.parse(field, filter.getType(), field + "_" + index, valueFilters.get(index));
            concat(builder);
        });
    }
}
