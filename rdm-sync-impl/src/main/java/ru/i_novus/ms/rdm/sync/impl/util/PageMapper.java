package ru.i_novus.ms.rdm.sync.impl.util;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.data.domain.Page;

import java.util.function.Function;
import java.util.stream.Collectors;

public class PageMapper {
    /**
     * т.к в RestPage не корректно работает метод map то делаем такой костыль
     */
    public static <T, R> Page<R> map(Page<T> page, Function<T, R> mapper) {
        if (page == null || page.getContent().isEmpty()) {
            return Page.empty();
        } else {
            return new RestPage<>(page.getContent().stream().map(mapper).collect(Collectors.toList()));
        }

    }

}
