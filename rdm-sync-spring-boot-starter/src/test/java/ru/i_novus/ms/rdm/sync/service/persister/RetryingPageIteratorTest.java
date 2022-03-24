package ru.i_novus.ms.rdm.sync.service.persister;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.sync.service.persister.NotSimpleVersionedPersisterServiceTest.createFirstRdmData;

@RunWith(MockitoJUnitRunner.class)
public class RetryingPageIteratorTest {

    @Mock
    private Iterator<Page> original;

    @Test
    public void testIteratorReturnExceptionAndTryAgain() {

        Page<Map<String, Object>> data = createFirstRdmData();

        when(original.next())
                .thenThrow(HttpClientErrorException.class)
                .thenThrow(HttpClientErrorException.class)
                .thenThrow(HttpClientErrorException.class)
                .thenReturn(data);

        RetryingPageIterator retryingPageIterator = new RetryingPageIterator(original, 10, 1000);

        assertEquals(data, retryingPageIterator.next());

    }

}