package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.RowValueMixin;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public void  init() {
        objectMapper.addMixIn(RowValue.class, TestRowValueMixin.class);
    }

    @Bean
    public RefBookService refBookService() throws IOException {
        RefBookService refBookService = mock(RefBookService.class);
        RefBook ek002 = objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002.json"), "UTF-8"), RefBook.class);
        when(refBookService.search(any(RefBookCriteria.class))).thenReturn(new RestPage<>(Collections.singletonList(ek002)));
        return refBookService;
    }

    @Bean
    public VersionRestService versionService() throws IOException {
        VersionRestService versionService = mock(VersionRestService.class);
        RefBookRowValue[] rows = objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002-data.json"), "UTF-8"), RefBookRowValue[].class);
        when(versionService.search(eq("EK002"), argThat(searchDataCriteria -> searchDataCriteria.getPageNumber() == 0))).thenReturn(new RestPage<>(Arrays.asList(rows)));
        when(versionService.search(eq("EK002"), argThat(searchDataCriteria -> searchDataCriteria.getPageNumber() == 1))).thenReturn(new RestPage<>(Collections.emptyList()));

        return versionService;
    }


}
