package ru.i_novus.ms.rdm.sync.impl;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public class AttributeTypeMapperTest {

    @Test
    public void testThatAllTypesWasMapped() {
        for (FieldType fieldType : FieldType.values()) {
            Assertions.assertNotNull(AttributeTypeMapper.map(fieldType), fieldType + " was not mapped");
        }
    }
}
