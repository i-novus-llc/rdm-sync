package ru.i_novus.ms.rdm.sync.util;

import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Page;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class PageIterator<T, C extends RestCriteria> implements Iterator<Page<T>> {
    private final Function<? super C, Page<T>> pageSource;
    private final C criteria;
    private int currentPage;
    private Page<T> nextPage;

    public PageIterator(Function<? super C, Page<T>> pageSource, C criteria) {
        this(pageSource, criteria, false);
    }

    public PageIterator(Function<? super C, Page<T>> pageSource, C criteria, boolean defaultSortProvied) {
        if (!defaultSortProvied && criteria.getSort() == null) {
            throw new IllegalArgumentException("You must either ensure that default sort is provided by pageSource or set some sorting in your criteria.");
        } else {
            this.pageSource = pageSource;
            this.criteria = criteria;
            this.currentPage = criteria.getPageNumber() - 1;
        }
    }

    public boolean hasNext() {
        if(nextPage != null) {
            return true;
        }
        this.criteria.setPageNumber(this.currentPage + 1);
        this.nextPage = this.pageSource.apply(this.criteria);
        List<? extends T> content = this.nextPage.getContent();
        return !content.isEmpty();
    }

    public Page<T> next() {
        if(!hasNext()){
            throw new NoSuchElementException();
        }
        Page<T>  result;
        if (this.nextPage != null) {
            result = this.nextPage;
            this.nextPage = null;
        } else {
            this.criteria.setPageNumber(this.currentPage + 1);
            result = pageSource.apply(this.criteria);
        }

        ++this.currentPage;
        return result;
    }
}

