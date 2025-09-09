package ru.i_novus.ms.rdm.sync.dao.rsql;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;

import java.util.HashSet;
import java.util.Set;

public class CustomRsqlParser {

    // Определяем кастомные операторы
    public static final ComparisonOperator LIKE = new ComparisonOperator("=like=");
    public static final ComparisonOperator ILIKE = new ComparisonOperator("=ilike=");
    public static final ComparisonOperator QLIKE = new ComparisonOperator("=qlike=");
    public static final ComparisonOperator IQLIKE = new ComparisonOperator("=iqlike=");
    public static final ComparisonOperator NULL = new ComparisonOperator("=null=");
    public static final ComparisonOperator NOT_NULL = new ComparisonOperator("=notnull=");

    // Собираем все операторы вместе
    public static final Set<ComparisonOperator> DEFAULT_OPERATORS = new HashSet<>();

    static {
        // Стандартные операторы
        DEFAULT_OPERATORS.addAll(RSQLOperators.defaultOperators());

        // Кастомные операторы
        DEFAULT_OPERATORS.add(LIKE);
        DEFAULT_OPERATORS.add(ILIKE);
        DEFAULT_OPERATORS.add(QLIKE);
        DEFAULT_OPERATORS.add(IQLIKE);
        DEFAULT_OPERATORS.add(NULL);
        DEFAULT_OPERATORS.add(NOT_NULL);
    }

    public static Node parse(String rsql) {
        return new RSQLParser(DEFAULT_OPERATORS).parse(rsql);
    }
}