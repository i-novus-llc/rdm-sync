package ru.i_novus.ms.rdm.sync.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MappingDataTransformerTest {

    private MappingDataTransformer dataTransformer;

    @BeforeEach
    void setUp() {
        dataTransformer = new MappingDataTransformer();
    }

    @Test
    void testInterpretBooleanTrue() {
        Object result = dataTransformer.evaluateExpression("interpretBoolean", 1, Boolean.class);
        assertTrue(result instanceof Boolean && (Boolean) result, "Expected true for input 1");
    }

    @Test
    void testInterpretBooleanFalse() {
        Object result = dataTransformer.evaluateExpression("interpretBoolean", 0, Boolean.class);
        assertFalse(result instanceof Boolean && (Boolean) result, "Expected false for input 0");
    }

    @Test
    void testUnknownExpression() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                dataTransformer.evaluateExpression("unknownExpression", 1, Boolean.class));

        assertEquals("Expression not found for key: unknownExpression", exception.getMessage());
    }

    @Test
    void testNullInput() {
        Object result = dataTransformer.evaluateExpression("interpretBoolean", null, Boolean.class);
        assertFalse(result instanceof Boolean && (Boolean) result, "Expected false for null input");
    }
}
