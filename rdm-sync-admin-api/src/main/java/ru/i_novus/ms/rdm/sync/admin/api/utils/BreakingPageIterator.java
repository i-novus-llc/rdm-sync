package ru.i_novus.ms.rdm.sync.admin.api.utils;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import java.util.function.Function;

import static ru.i_novus.ms.rdm.sync.admin.api.utils.CriteriaUtils.createEmptyPage;

/**
 * Прерываемый итератор страниц.
 *
 * @param <T> класс значения на странице
 * @param <C> класс критерия поиска
 */
public class BreakingPageIterator<T, C extends AbstractCriteria> extends PageIterator<T, C> {

    private final Function<? super C, Boolean> isBreaking;

    public BreakingPageIterator(Function<? super C, Page<? extends T>> pageSource, C criteria,
                                Function<? super C, Boolean> isBreaking) {
        super(pageSource, criteria);

        this.isBreaking = isBreaking;
    }

    @Override
    protected Page<? extends T> getNextPage() {

        if (isBreaking != null && isBreaking.apply(getCriteria()))
            return createEmptyPage(getCriteria());

        return super.getNextPage();
    }
}
