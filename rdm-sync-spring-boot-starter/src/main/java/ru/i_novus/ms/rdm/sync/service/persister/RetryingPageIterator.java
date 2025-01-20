package ru.i_novus.ms.rdm.sync.service.persister;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class RetryingPageIterator<T> implements Iterator<Page<T>> {

    private static final Logger logger = LoggerFactory.getLogger(RetryingPageIterator.class);

    private static final String LOG_TRY_AGAIN_WARN = "We will try again in %s seconds (%s tries left)";

    private final Iterator<Page<T>> original;
    private final int tries;
    private final int timeout;

    public RetryingPageIterator(Iterator<Page<T>> original, int tries, int timeout) {
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
    public Page<T> next() {
        if(!hasNext()){
            throw new NoSuchElementException();
        }
        return retry(original::next);
    }

    @SneakyThrows
    private <E> E retry(Supplier<? extends E> supplier){
        int count = 0;
        while (count++ < tries) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                logger.error("Cannot load page", e);
                logger.warn(String.format(LOG_TRY_AGAIN_WARN, timeout / 1000, tries - count));
                Thread.sleep(timeout);
            }
        }
        return supplier.get();
    }


}
