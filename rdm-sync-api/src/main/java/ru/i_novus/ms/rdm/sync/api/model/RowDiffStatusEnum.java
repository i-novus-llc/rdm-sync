package ru.i_novus.ms.rdm.sync.api.model;

import java.util.HashMap;
import java.util.Map;

public enum RowDiffStatusEnum {

    INSERTED,
    UPDATED,
    DELETED;

    private static final Map<String, RowDiffStatusEnum> STATUS_MAP = new HashMap<>();
    static {
        for (RowDiffStatusEnum type : RowDiffStatusEnum.values()) {
            STATUS_MAP.put(type.name(), type);
        }
    }

    /**
     * Получение типа по строковому значению типа.
     * <p/>
     * Значение null может (но не должно) прийти из RDM.
     * Обычный {@link RowDiffStatusEnum#valueOf} не подходит, т.к. кидает исключение.
     *
     * @param value Строковое значение
     * @return Тип атрибута
     */
    public static RowDiffStatusEnum fromValue(String value) {

        return value != null ? STATUS_MAP.get(value) : null;
    }
}
