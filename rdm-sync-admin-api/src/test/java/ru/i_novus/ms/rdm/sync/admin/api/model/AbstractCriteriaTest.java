package ru.i_novus.ms.rdm.sync.admin.api.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.sync.admin.api.BaseTest;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class AbstractCriteriaTest extends BaseTest {

    @Test
    public void testClass() {

        AbstractCriteria criteria = new AbstractCriteria();
        assertNotNull(criteria);
        assertSpecialEquals(criteria);

        AbstractCriteria copyCriteria = new AbstractCriteria(criteria);
        assertObjects(Assertions::assertEquals, criteria, copyCriteria);

        AbstractCriteria sameCriteria = new AbstractCriteria(criteria.getPageNumber(), criteria.getPageSize());
        assertObjects(Assertions::assertEquals, criteria, sameCriteria);
    }

    @Test
    public void testPaging() {

        AbstractCriteria criteria = new AbstractCriteria();

        AbstractCriteria unpagedCriteria = new AbstractCriteria();
        unpagedCriteria.makeUnpaged();
        assertTrue(unpagedCriteria.madeUnpaged());
        assertObjects(Assertions::assertNotEquals, criteria, unpagedCriteria);

        AbstractCriteria pagedCriteria = new AbstractCriteria(1000, 1000);
        assertFalse(pagedCriteria.madeUnpaged());
        assertObjects(Assertions::assertNotEquals, criteria, pagedCriteria);
        assertObjects(Assertions::assertNotEquals, unpagedCriteria, pagedCriteria);
    }

    @Test
    public void testSorting() {

        AbstractCriteria criteria = new AbstractCriteria();

        Sort.Order idOrder = new Sort.Order(Sort.Direction.ASC, "id");
        List<Sort.Order> orders = singletonList(idOrder);

        AbstractCriteria sortedCriteria = new AbstractCriteria();
        sortedCriteria.setOrders(orders);
        assertEquals(orders, sortedCriteria.getOrders());
        assertObjects(Assertions::assertNotEquals, criteria, sortedCriteria);
    }
}