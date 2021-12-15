package ru.i_novus.ms.rdm.sync.dao.builder;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Построитель условий для SQL.
 */
public class SqlClauseBuilder implements ClauseBuilder {

    private final List<String> clauses = new ArrayList<>();

    private final Map<String, Serializable> params = new HashMap<>();

    public SqlClauseBuilder() {
        
    }

    public SqlClauseBuilder(List<String> clauses, Map<String, Serializable> params) {

        if (!isEmpty(clauses)) {
            this.clauses.addAll(clauses);
        }

        if (!isEmpty(params)) {
            this.params.putAll(params);
        }
    }

    @Override
    public List<String> getClauses() {
        return clauses;
    }

    @Override
    public Map<String, Serializable> getParams() {
        return params;
    }

    @Override
    public String build() {
        return collect(Collectors.joining());
    }

    protected <A> String collect(Collector<CharSequence, A, String> collector) {

        return clauses.stream().collect(collector);
    }

    @Override
    public SqlClauseBuilder append(String clause) {

        if (!StringUtils.isEmpty(clause)) {
            this.clauses.add(clause);
        }

        return this;
    }

    @Override
    public SqlClauseBuilder append(List<String> clauses) {

        if (!isEmpty(clauses)) {
            this.clauses.addAll(clauses);
        }

        return this;
    }

    @Override
    public SqlClauseBuilder bind(String name, Serializable value) {

        if (!StringUtils.isEmpty(name) && value != null) {
            this.params.put(name, value);
        }

        return this;
    }

    @Override
    public SqlClauseBuilder bind(Map.Entry<String, Serializable> param) {

        return (param != null) ? bind(param.getKey(), param.getValue()) : this;
    }

    @Override
    public SqlClauseBuilder bind(Map<String, Serializable> params) {

        if (!isEmpty(params)) {
            this.params.putAll(params);
        }

        return this;
    }

    @Override
    public void concat(String clause, Map<String, Serializable> params) {

        append(clause);
        bind(params);
    }

    @Override
    public void concat(ClauseBuilder builder) {

        if (builder == null)
            return;
        
        String clause = builder.build();
        concat(clause, builder.getParams());
    }
}
