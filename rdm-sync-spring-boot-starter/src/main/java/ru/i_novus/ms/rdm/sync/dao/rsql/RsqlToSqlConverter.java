package ru.i_novus.ms.rdm.sync.dao.rsql;

import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RsqlToSqlConverter {

    private static final Map<String, String> OPERATOR_MAP = Map.ofEntries(
            Map.entry("==", "="),
            Map.entry("!=", "<>"),
            Map.entry("=gt=", ">"),
            Map.entry("=ge=", ">="),
            Map.entry("=lt=", "<"),
            Map.entry("=le=", "<="),
            Map.entry("=in=", "IN"),
            Map.entry("=out=", "NOT IN"),
            Map.entry("=null=", "IS NULL"),
            Map.entry("=notnull=", "IS NOT NULL"),
            Map.entry("=like=", "LIKE"),
            Map.entry("=ilike=", "ILIKE"),   // ILIKE - case insensitive
            Map.entry("=qlike=", "LIKE"),    // QLIKE - ignoring quotes
            Map.entry("=iqlike=", "ILIKE")   // IQLIKE - case insensitive + ignoring quotes
    );

    private static final Pattern QUOTES_PATTERN = Pattern.compile("[\"']");
    private static final Pattern WILDCARD_PATTERN = Pattern.compile("\\*");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("['%;]");

    public static String convertToSql(String rsqlFilter) {
        if (StringUtils.isBlank(rsqlFilter)) {
            return "";
        }

        try {
            // Парсим RSQL
            Node rootNode = CustomRsqlParser.parse(rsqlFilter);
            return convertNodeToSql(rootNode);

        } catch (RSQLParserException e) {
            throw new RuntimeException("Failed to parse RSQL: " + rsqlFilter, e);
        }
    }

    private static String convertNodeToSql(Node node) {
        if (node instanceof ComparisonNode) {
            return convertComparisonNode((ComparisonNode) node);
        } else if (node instanceof LogicalNode) {
            return convertLogicalNode((LogicalNode) node);
        }
        throw new IllegalArgumentException("Unsupported node type: " + node.getClass());
    }

    private static String convertComparisonNode(ComparisonNode node) {
        String field = node.getSelector();
        String operator = node.getOperator().getSymbol();
        List<String> arguments = node.getArguments();

        String sqlOperator = OPERATOR_MAP.get(operator);
        if (sqlOperator == null) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        // Особые случаи для NULL операторов
        if ("=null=".equals(operator) || "=notnull=".equals(operator)) {
            if (!arguments.isEmpty() && !"null".equalsIgnoreCase(arguments.get(0))) {
                throw new IllegalArgumentException("NULL operators should have 'null' as value");
            }
            return field + " " + sqlOperator;
        }

        // Обработка QLIKE и IQLIKE
        if ("=qlike=".equals(operator) || "=iqlike=".equals(operator)) {
            validateLikeOperator(arguments, operator);
            String processedValue = processQuotes(arguments.get(0), operator);
            processedValue = processCase(processedValue, operator);
            String likePattern = convertToLikePattern(processedValue);
            return field + " " + sqlOperator + " " + likePattern;
        }

        // Особые случаи для LIKE операторов
        if ("=like=".equals(operator) || "=ilike=".equals(operator)) {
            validateLikeOperator(arguments, operator);
            String processedValue = processCase(arguments.get(0), operator);
            String likePattern = convertToLikePattern(processedValue);
            return field + " " + sqlOperator + " " + likePattern;
        }

        // Экранирование значений
        String sqlValue = convertValuesToSql(arguments, operator);

        return field + " " + sqlOperator + " " + sqlValue;
    }

    private static void validateLikeOperator(List<String> arguments, String operator) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("LIKE operator requires exactly one argument");
        }

        String value = arguments.get(0);
        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
            throw new SecurityException("Potential SQL injection detected in LIKE value");
        }
    }

    private static String convertToLikePattern(String value) {
        // Преобразование RSQL wildcards в SQL LIKE patterns:
        // * → %
        // ? → _ (если нужно)
        String likePattern = WILDCARD_PATTERN.matcher(value).replaceAll("%");
        return "'" + likePattern.replace("'", "''") + "'";
    }

    private static String processQuotes(String value, String operator) {
        // Удаляем кавычки
       return QUOTES_PATTERN.matcher(value).replaceAll("");
    }

    private static String processCase(String value, String operator) {
        // Для ILIKE и IQLIKE приводим к нижнему регистру
        if ("=ilike=".equals(operator) || "=iqlike=".equals(operator)) {
            return value.toLowerCase();
        }
        return value;
    }

    private static String convertLogicalNode(LogicalNode node) {
        List<String> childrenSql = node.getChildren().stream()
                .map(RsqlToSqlConverter::convertNodeToSql)
                .collect(Collectors.toList());

        String operator = node.getOperator() == LogicalOperator.AND ? " AND " : " OR ";
        return "(" + String.join(operator, childrenSql) + ")";
    }

    private static String convertValuesToSql(List<String> values, String operator) {
        if ("=in=".equals(operator) || "=out=".equals(operator)) {
            // Обработка списков значений
            List<String> quotedValues = values.stream()
                    .map(value -> convertSingleValueToSql(value, operator))
                    .collect(Collectors.toList());
            return "(" + String.join(",", quotedValues) + ")";
        } else if (values.size() == 1) {
            // Одиночное значение
            return convertSingleValueToSql(values.get(0), operator);
        } else {
            throw new IllegalArgumentException("Multiple values not supported for operator: " + operator);
        }
    }

    private static String convertSingleValueToSql(String value, String operator) {
        if (isNumeric(value)) {
            return value; // Числа без кавычек
        } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return value; // Булевы значения без кавычек
        } else if ("null".equalsIgnoreCase(value)) {
            return "NULL"; // NULL значение
        } else {
            return "'" + value.replace("'", "''") + "'"; // Строки в кавычках
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }


}
