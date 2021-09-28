package ru.i_novus.ms.rdm.sync.admin.api.utils;

import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Test;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.sync.admin.api.utils.CriteriaUtils.createEmptyPage;
import static ru.i_novus.ms.rdm.sync.admin.api.utils.CriteriaUtils.isUnpaged;

public class CriteriaUtilsTest {

    @Test
    public void testIsUnpaged() {

        AbstractCriteria criteria = new AbstractCriteria();
        assertFalse(isUnpaged(criteria));

        AbstractCriteria unpagedCriteria = new AbstractCriteria();
        unpagedCriteria.makeUnpaged();
        assertTrue(isUnpaged(unpagedCriteria));
    }

    @Test
    public void testIsUnpagedWhenZero() {

        try {
            new AbstractCriteria(0, 0);
            fail(IllegalArgumentException.class.getSimpleName() + " error expected");

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Page size must not be less than one!", e.getMessage());
        }
    }

    @Test
    public void testCreateEmptyPage() {

        AbstractCriteria criteria = new AbstractCriteria();
        RestPage<String> page = createEmptyPage(criteria);
        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
        assertEquals(emptyList(), page.getContent());
    }
}