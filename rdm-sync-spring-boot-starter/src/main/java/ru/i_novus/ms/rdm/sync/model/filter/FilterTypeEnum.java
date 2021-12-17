package ru.i_novus.ms.rdm.sync.model.filter;

import java.util.*;

/**
 * Тип фильтра значений для поиска.
 */
public enum FilterTypeEnum {

    EQUAL("eq"),
    LIKE("like"),
    IS_NULL("null"),
    IS_NOT_NULL("not-null")
    ;

    /** Список типов фильтров для оптимизации доступа. */
    private static final List<FilterTypeEnum> TYPE_LIST = Arrays.asList(FilterTypeEnum.values());

    /** Набор соответствий масок и типов фильтров. */
    private static final Map<String, FilterTypeEnum> MASK_MAP = new HashMap<>();

    static {
        for (FilterTypeEnum type : FilterTypeEnum.values()) {
            MASK_MAP.put(type.mask, type);
        }
    }

    /** Типы фильтров, не требующие значений. */
    private static final Set<FilterTypeEnum> NON_VALUED = Set.of(IS_NULL, IS_NOT_NULL);

    /** Маска типа фильтра в значении для поиска. */
    private final String mask;

    FilterTypeEnum(String mask) {
        this.mask = mask;
    }

    public String getMask() {
        return mask;
    }

    /**
     * Получение типа фильтра по строковому значению типа (без выбрасывания исключения).
     *
     * @param mask маска типа фильтра
     * @return Тип фильтра или null
     */
    public static FilterTypeEnum fromMask(String mask) {

        return mask != null ? MASK_MAP.get(mask) : null;
    }

    /**
     * Проверка на необходимость значений для типа.
     *
     * @return Результат проверки
     */
    public boolean isValued() {
        return !NON_VALUED.contains(this);
    }

    public static int size() {
        return TYPE_LIST.size();
    }

    public static List<FilterTypeEnum> list() {
        return TYPE_LIST;
    }
}
