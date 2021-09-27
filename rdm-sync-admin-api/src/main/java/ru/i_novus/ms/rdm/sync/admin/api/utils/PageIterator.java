package ru.i_novus.ms.rdm.sync.admin.api.utils;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class PageIterator<T, C extends AbstractCriteria> implements Iterator<Page<? extends T>> {

    private final Function<? super C, Page<? extends T>> pageSource;

    private final C criteria;

    private int currentPage;

    private Page<? extends T> nextPage;

    public PageIterator(Function<? super C, Page<? extends T>> pageSource, C criteria) {

        this.pageSource = pageSource;
        this.criteria = criteria;
        this.currentPage = criteria.getPageNumber() - 1;
    }

    @Override
    public boolean hasNext() {

        criteria.setPageNumber(currentPage + 1);

        nextPage = pageSource.apply(criteria);
        List<? extends T> content = nextPage.getContent();

        return !content.isEmpty();
    }

    @Override
    @SuppressWarnings("squid:S2272")
    public Page<? extends T> next() {

        Page<? extends T> result;

        if (nextPage != null) {
            result = nextPage;
            nextPage = null;

        } else {
            criteria.setPageNumber(currentPage + 1);
            result = pageSource.apply(criteria);
        }
        currentPage++;

        return result;
    }
}
