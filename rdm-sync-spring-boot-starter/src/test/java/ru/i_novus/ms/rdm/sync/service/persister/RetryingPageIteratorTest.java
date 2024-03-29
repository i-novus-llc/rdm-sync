package ru.i_novus.ms.rdm.sync.service.persister;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RetryingPageIteratorTest {

    @Mock
    private Iterator<Page> original;

    @Test
    public void testIteratorReturnExceptionAndTryAgain() {

        Page page = mock(Page.class);

        when(original.next())
                .thenThrow(HttpClientErrorException.class)
                .thenThrow(HttpClientErrorException.class)
                .thenThrow(HttpClientErrorException.class)
                .thenReturn(page);

        when(original.hasNext())
                .thenReturn(true);

        RetryingPageIterator retryingPageIterator = new RetryingPageIterator(original, 10, 1000);

        assertEquals(page, retryingPageIterator.next());

    }

}