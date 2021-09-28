package ru.i_novus.ms.rdm.sync.admin.api.utils;

import net.n2oapp.platform.jaxrs.RestPage;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import static java.util.Collections.emptyList;

public class CriteriaUtils {

    private CriteriaUtils() {
    }

    /**
     * Проверка критерия поиска на нестраничность.
     *
     * @param criteria критерий поиска
     * @return Результат проверки
     */
    public static boolean isUnpaged(AbstractCriteria criteria) {

        return criteria.getPageNumber() == 0 &&
                (criteria.getPageSize() == 0 || criteria.getPageSize() == Integer.MAX_VALUE);
    }

    /**
     * Создание пустой страницы (при отсутствии данных).
     *
     * @param criteria критерий
     * @param <T>      класс
     * @return Пустая страница
     */
    public static <T> RestPage<T> createEmptyPage(AbstractCriteria criteria) {

        return new RestPage<>(emptyList(), criteria, 0);
    }
}
