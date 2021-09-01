package ru.i_novus.ms.rdm.sync.admin.api.model;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.sync.admin.api.BaseTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RdmSyncSourceTest extends BaseTest {

    @Test
    public void testEmpty() {

        RdmSyncSource empty = new RdmSyncSource();
        assertSpecialEquals(empty);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testClass() {

        RdmSyncSource empty = new RdmSyncSource();
        RdmSyncSource model = createModel();
        assertFalse(model.isEmpty());

        assertObjects(Assert::assertNotEquals, empty, model);
    }

    @Test
    public void testCopy() {

        RdmSyncSource model = createModel();
        RdmSyncSource copyModel = createCopy(model);

        assertObjects(Assert::assertEquals, model, copyModel);
    }

    private RdmSyncSource createModel() {

        RdmSyncSource model = new RdmSyncSource();
        model.setCode("code");
        model.setName("name");
        model.setCaption("caption");
        model.setLink("link");
        model.setToken("token");

        return model;
    }

    private RdmSyncSource createCopy(RdmSyncSource that) {

        if (that == null)
            return null;

        RdmSyncSource model = new RdmSyncSource();
        model.setCode(that.getCode());
        model.setName(that.getName());
        model.setCaption(that.getCaption());
        model.setLink(that.getLink());
        model.setToken(that.getToken());

        return model;
    }
}