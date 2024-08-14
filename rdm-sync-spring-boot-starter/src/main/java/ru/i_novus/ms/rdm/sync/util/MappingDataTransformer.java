package ru.i_novus.ms.rdm.sync.util;

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

    // Парсер для SpEL выражений
    private final ExpressionParser parser = new SpelExpressionParser();

    public Object evaluateExpression(String expr, Object input, Class<?> returnType) {

        // Создание контекста для оценки SpEL выражения
        SimpleEvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withRootObject(input)
                .build();

        return parser.parseExpression(expr).getValue(context, returnType);
    }

}
