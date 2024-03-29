package ru.i_novus.ms.rdm.sync.service;

import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public interface RdmMappingService {
    /**
     * Преобразование объекта согласно маппингу.
     *
     * @param attributeType тип данных в НСИ
     * @param sysType тип данных в системе
     * @param value        значение для преобразования
     * @return преобразованное значение
     */
    Object map(AttributeTypeEnum attributeType, DataTypeEnum sysType, Object value);
}
