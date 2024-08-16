package ru.i_novus.ms.rdm.sync.service;

import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public interface RdmMappingService {
    /**
     * Преобразование объекта согласно маппингу.
     *
     * @param attributeType тип данных в НСИ
     * @param fieldMapping маппинг полей
     * @param value        значение для преобразования
     * @return преобразованное значение
     */
    Object map(AttributeTypeEnum attributeType, FieldMapping fieldMapping, Object value);
}
