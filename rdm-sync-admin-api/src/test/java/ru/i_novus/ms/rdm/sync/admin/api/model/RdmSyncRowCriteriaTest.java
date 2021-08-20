package ru.i_novus.ms.rdm.sync.admin.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.sync.admin.api.BaseTest;

public class RdmSyncRowCriteriaTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testEmpty() {

        RdmSyncRowCriteria empty = new RdmSyncRowCriteria();
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        RdmSyncRowCriteria empty = new RdmSyncRowCriteria();
        RdmSyncRowCriteria criteria = createCriteria();

        assertObjects(Assert::assertNotEquals, empty, criteria);
    }

    @Test
    public void testCopy() {

        RdmSyncRowCriteria criteria = createCriteria();
        RdmSyncRowCriteria copyCriteria = createCopy(criteria);

        assertObjects(Assert::assertEquals, criteria, copyCriteria);
    }

    private RdmSyncRowCriteria createCriteria() {

        RdmSyncRowCriteria criteria = new RdmSyncRowCriteria();
        criteria.setCode("code");
        criteria.setName("name");
        criteria.setText("text");

        criteria.setCount(1);

        return criteria;
    }

    private RdmSyncRowCriteria createCopy(RdmSyncRowCriteria that) {

        if (that == null)
            return null;

        RdmSyncRowCriteria criteria = new RdmSyncRowCriteria();
        criteria.setCode(that.getCode());
        criteria.setName(that.getName());
        criteria.setText(that.getText());

        criteria.setCount(that.getCount());

        return criteria;
    }
}