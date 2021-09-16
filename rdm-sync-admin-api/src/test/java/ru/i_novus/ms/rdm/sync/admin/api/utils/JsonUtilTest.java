package ru.i_novus.ms.rdm.sync.admin.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import static org.junit.Assert.*;

public class JsonUtilTest {

    @Test
    public void testJsonString() {

        AbstractCriteria oldCriteria = new AbstractCriteria();

        String value = JsonUtil.toJsonString(oldCriteria);
        assertNotNull(value);

        AbstractCriteria newCriteria = JsonUtil.fromJsonString(value, AbstractCriteria.class);
        assertNotNull(newCriteria);

        assertEquals(oldCriteria, newCriteria);
    }

    @Test
    public void testJsonStringWhenError() {

        ObjectMapper testMapper = new TestObjectMapper();

        try {
            JsonUtil.toJsonString(testMapper, new AbstractCriteria());
            fail();

        } catch (RuntimeException e) {

            assertEquals(IllegalArgumentException.class, e.getClass());
        }

        try {
            JsonUtil.fromJsonString(testMapper, "{}", AbstractCriteria.class);
            fail();

        } catch (RuntimeException e) {

            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    private static class TestObjectMapper extends ObjectMapper {

        @Override
        public <T> T readValue(String content, Class<T> valueType) throws TestJsonProcessingException {

            throw new TestJsonProcessingException("Test exception for readValue");
        }

        @Override
        public String writeValueAsString(Object value) throws TestJsonProcessingException {

            throw new TestJsonProcessingException("Test exception for writeValueAsString");
        }
    }

    private static class TestJsonProcessingException extends JsonProcessingException {

        public TestJsonProcessingException(String msg) {
            super(msg);
        }
    }
}