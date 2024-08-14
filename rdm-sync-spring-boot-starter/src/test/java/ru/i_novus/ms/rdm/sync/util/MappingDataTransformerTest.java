package ru.i_novus.ms.rdm.sync.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MappingDataTransformerTest {

    private final MappingDataTransformer dataTransformer = new MappingDataTransformer();

    @Test
    void testInterpretBoolean() {
        // Тестирование различных входных значений для выражения 'interpretBoolean'
        Object result = dataTransformer.evaluateExpression("interpretBoolean", 1, Boolean.class);
        assertTrue(result instanceof Boolean && (Boolean) result, "Expected true for input 1");

        result = dataTransformer.evaluateExpression("interpretBoolean", 0, Boolean.class);
        assertFalse(result instanceof Boolean && (Boolean) result, "Expected false for input 0");

        result = dataTransformer.evaluateExpression("interpretBoolean", "истина", Boolean.class);
        assertTrue(result instanceof Boolean && (Boolean) result, "Expected true for input 'истина'");

        result = dataTransformer.evaluateExpression("interpretBoolean", "ложь", Boolean.class);
        assertFalse(result instanceof Boolean && (Boolean) result, "Expected false for input 'ложь'");

        result = dataTransformer.evaluateExpression("interpretBoolean", null, Boolean.class);
        assertNull(result, "Expected null for input null");
    }

    @Test
    void testUnknownExpression() {
        try {
            dataTransformer.evaluateExpression("unknownExpression", "someValue", Object.class);
            fail("Expected IllegalArgumentException for unknown expression key");
        } catch (IllegalArgumentException e) {
            assertEquals("Expression not found for key: unknownExpression", e.getMessage());
        }
    }
}
