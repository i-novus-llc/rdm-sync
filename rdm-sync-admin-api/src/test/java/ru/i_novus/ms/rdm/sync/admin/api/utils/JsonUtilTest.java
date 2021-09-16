package ru.i_novus.ms.rdm.sync.admin.api.utils;

import org.junit.Test;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}