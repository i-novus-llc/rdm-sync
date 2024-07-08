package ru.i_novus.ms.rdm.sync.dao.builder;

import java.util.List;
import java.util.Map;

/**
 * Построитель условий.
 */
public interface ClauseBuilder {

    List<String> getClauses();
    Map<String, Object> getParams();

    ClauseBuilder append(String clause);
    ClauseBuilder append(List<String> clauses);

    ClauseBuilder bind(String name, Object value);
    ClauseBuilder bind(Map<String, Object> params);

    void concat(String clause, Map<String, Object> params);
    void concat(ClauseBuilder builder);

    String build();
}
