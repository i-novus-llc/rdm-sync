package ru.i_novus.ms.rdm.sync.impl;

import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public class AttributeTypeMapper {

    private AttributeTypeMapper() {
    }

    public static AttributeTypeEnum map(FieldType fieldType) {
        return AttributeTypeEnum.valueOf(fieldType.name());
    }

}
