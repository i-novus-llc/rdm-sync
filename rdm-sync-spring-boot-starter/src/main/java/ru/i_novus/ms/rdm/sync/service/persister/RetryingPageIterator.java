package ru.i_novus.ms.rdm.sync.service.persister;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.util.Iterator;
import java.util.function.Supplier;

public class RetryingPageIterator<T> implements Iterator<Page<? extends T>> {

    private final static Logger logger = LoggerFactory.getLogger(RetryingPageIterator.class);

    private final Iterator<Page<? extends T>> original;
    private final int tries;
    private final int timeout;

    public RetryingPageIterator(Iterator<Page<? extends T>> original, int tries, int timeout) {
        this.original = original;
        this.timeout = timeout;
        this.tries = tries;
    }

    @SneakyThrows
    @Override
    public boolean hasNext() {
        return retry(original::hasNext);
    }

    @SneakyThrows
    @Override
    public Page<? extends T> next(){
        return retry(original::next);
    }

    @SneakyThrows
    private <E> E retry(Supplier<? extends E> supplier){
        int count = 0;
        while (count++ < tries) {
            try {
                return supplier.get();
            } catch (Throwable e) {
                logger.warn(String.format("An error occurred, we will try again in %s seconds (%s tries left)", timeout / 1000, tries - count));
                Thread.sleep(timeout);
            }
        }
        return supplier.get();
    }


}
