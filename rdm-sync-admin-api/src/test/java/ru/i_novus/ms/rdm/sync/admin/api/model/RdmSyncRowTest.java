package ru.i_novus.ms.rdm.sync.admin.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.sync.admin.api.BaseTest;
import ru.i_novus.ms.rdm.sync.admin.api.JsonUtil;

import java.time.LocalDateTime;

public class RdmSyncRowTest extends BaseTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testEmpty() {

        RdmSyncRow empty = new RdmSyncRow();
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        RdmSyncRow empty = new RdmSyncRow();
        RdmSyncRow model = createModel();

        assertObjects(Assert::assertNotEquals, empty, model);
    }

    @Test
    public void testCopy() {

        RdmSyncRow model = createModel();
        RdmSyncRow copyModel = createCopy(model);

        assertObjects(Assert::assertEquals, model, copyModel);
    }

    private RdmSyncRow createModel() {

        RdmSyncRow model = new RdmSyncRow();
        model.setCode("code");
        model.setName("name");

        model.setVersion("1.2");
        model.setVersioned(true);
        model.setAutoUpdatable(false);

        model.setSourceType("test");
        model.setLastDateTime(LocalDateTime.now());
        model.setLastStatus("ok");

        return model;
    }

    private RdmSyncRow createCopy(RdmSyncRow that) {

        if (that == null)
            return null;

        RdmSyncRow model = new RdmSyncRow();
        model.setCode(that.getCode());
        model.setName(that.getName());

        model.setVersion(that.getVersion());
        model.setVersioned(that.getVersioned());
        model.setAutoUpdatable(that.getAutoUpdatable());

        model.setSourceType(that.getSourceType());
        model.setLastDateTime(that.getLastDateTime());
        model.setLastStatus(that.getLastStatus());

        return model;
    }
}