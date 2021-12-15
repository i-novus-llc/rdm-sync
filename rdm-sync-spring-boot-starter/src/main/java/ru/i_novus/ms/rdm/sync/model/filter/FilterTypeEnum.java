package ru.i_novus.ms.rdm.sync.model.filter;

import java.util.*;

/**
 * Тип фильтра значений для поиска.
 */
public enum FilterTypeEnum {

    EQUAL("eq"),
    LIKE("like")
    ;

    private static final List<FilterTypeEnum> TYPE_LIST = Arrays.asList(FilterTypeEnum.values());

    private static final Map<String, FilterTypeEnum> MASK_MAP = new HashMap<>();

    static {
        for (FilterTypeEnum type : FilterTypeEnum.values()) {
            MASK_MAP.put(type.mask, type);
        }
    }

    /** Маска типа фильтра в значении для поиска. */
    private final String mask;

    public String getMask() {
        return mask;
    }

    /**
     * Получение типа по строковому значению типа без исключения.
     *
     * @param mask маска типа фильтра
     * @return Тип или null
     */
    public static FilterTypeEnum fromMask(String mask) {

        return mask != null ? MASK_MAP.get(mask) : null;
    }


    FilterTypeEnum(String mask) {
        this.mask = mask;
    }

    public static int size() {
        return TYPE_LIST.size();
    }

    public static List<FilterTypeEnum> list() {
        return TYPE_LIST;
    }
}
