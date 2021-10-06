package ru.i_novus.ms.rdm.sync.admin.api.utils;

import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Test;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.sync.admin.api.utils.CriteriaUtils.*;

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
        Page<String> page = createEmptyPage(criteria);
        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
        assertEquals(emptyList(), page.getContent());
    }

    @Test
    public void testIsEmptyPage() {

        assertTrue(isEmptyPage(null));

        AbstractCriteria criteria = new AbstractCriteria();
        Page<String> page = createEmptyPage(criteria);
        assertTrue(isEmptyPage(page));
    }

    @Test
    public void testIsEmptyPageWhenEmptyList() {

        AbstractCriteria criteria = new AbstractCriteria();

        Page<String> page = new RestPage<>(emptyList(), criteria, 0);
        assertTrue(isEmptyPage(page));

        page = new RestPage<>(emptyList(), criteria, 1);
        assertTrue(isEmptyPage(page));
    }

    @Test
    public void testIsEmptyPageWhenNotEmptyList() {

        AbstractCriteria criteria = new AbstractCriteria();

        List<String> list = new ArrayList<>(1);
        list.add("one");

        Page<String> page = new RestPage<>(list, criteria, 0);
        assertTrue(isEmptyPage(page));

        page = new RestPage<>(list, criteria, list.size());
        assertFalse(isEmptyPage(page));
    }
}