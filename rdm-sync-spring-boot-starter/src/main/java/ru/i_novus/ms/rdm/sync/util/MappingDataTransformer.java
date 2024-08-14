package ru.i_novus.ms.rdm.sync.util;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилитный класс для трансформации данных на основе SpEL выражений.
 */
@Component
public class MappingDataTransformer {

    // Карта выражений, где ключ — это имя выражения, а значение — это строка SpEL выражения.
    private static final Map<String, String> expressionMap = new HashMap<>();

    // Парсер для SpEL выражений
    private final ExpressionParser parser = new SpelExpressionParser();

    static {
        // Инициализация карты выражений. Пример: интерпретация boolean значения.
        expressionMap.put("interpretBoolean",
                "#root == null ? null : \n" +
                "(#root == 1 || #root == 'истина') ? true : \n" +
                "(#root == 0 || #root == 'ложь') ? false : \n" +
                "false\n");
    }

    /**
     * Оценивает выражение по ключу и применяет его к входному значению.
     *
     * @param key         Ключ выражения, соответствующий определенному SpEL выражению.
     * @param input       Входное значение, которое будет использовано в выражении.
     * @param returnType  Ожидаемый тип возвращаемого значения.
     * @return Преобразованное значение на основе указанного выражения.
     * @throws IllegalArgumentException Если выражение по указанному ключу не найдено.
     */
    public Object evaluateExpression(String key, Object input, Class<?> returnType) {
        String expressionString = expressionMap.get(key);
        if (expressionString == null) {
            throw new IllegalArgumentException("Expression not found for key: " + key);
        }

        // Создание контекста для оценки SpEL выражения
        SimpleEvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withRootObject(input)
                .build();

        // Оценка выражения и возвращение результата
        Expression expression = parser.parseExpression(expressionString);
        return expression.getValue(context, returnType);
    }

}
