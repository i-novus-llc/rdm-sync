package ru.i_novus.ms.rdm.sync.impl;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public class AttributeTypeMapperTest {

    @Test
    public void testThatAllTypesWasMapped() {
        for (FieldType fieldType : FieldType.values()) {
            Assert.assertNotNull(fieldType + " was not mapped", AttributeTypeMapper.map(fieldType));
        }
    }
}
