package ru.i_novus.ms.rdm.sync.dao.builder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Построитель условий.
 */
public interface ClauseBuilder {

    List<String> getClauses();
    Map<String, Serializable> getParams();

    ClauseBuilder append(String clause);
    ClauseBuilder append(List<String> clauses);

    ClauseBuilder bind(String name, Serializable value);
    ClauseBuilder bind(Map.Entry<String, Serializable> param);
    ClauseBuilder bind(Map<String, Serializable> params);

    void concat(String clause, Map<String, Serializable> params);
    void concat(ClauseBuilder builder);

    String build();
}
