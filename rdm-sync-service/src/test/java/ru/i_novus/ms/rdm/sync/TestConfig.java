package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.io.IOUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.RowValueMixin;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.provider.RdmMapperConfigurer;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
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

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @PostConstruct
    public void  init() {
        objectMapper.addMixIn(RowValue.class, TestRowValueMixin.class);
    }

    @Bean
    public RefBookService refBookService() throws IOException {
        RefBookService refBookService = mock(RefBookService.class);
        RefBook ek002Ver1 = objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002_version1.json"), "UTF-8"), RefBook.class);
        RefBook ek002Ver2 = objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002_version2.json"), "UTF-8"), RefBook.class);
        when(refBookService.search(any(RefBookCriteria.class))).thenAnswer((Answer<Page<RefBook>>) invocationOnMock -> {
            RefBookCriteria refBookCriteria = invocationOnMock.getArgument(0, RefBookCriteria.class);
            if (refBookCriteria.getPageNumber() >= 1) {
                return new RestPage<>(Collections.emptyList());
            }
            VersionMapping ek002VersionMapping = rdmSyncDao.getVersionMapping("EK002");
            if(refBookCriteria.getCode().equals("EK002") && (ek002VersionMapping == null || !"1".equals(ek002VersionMapping.getVersion())) ) {
                return new RestPage<>(Collections.singletonList(ek002Ver1));
            }
            if (refBookCriteria.getCode().equals("EK002") && ek002VersionMapping != null || "1".equals(ek002VersionMapping.getVersion()))
                return new RestPage<>(Collections.singletonList(ek002Ver2));

            return new RestPage<>(Collections.emptyList());
        });
        return refBookService;
    }

    @Bean
    public VersionRestService versionService() throws IOException {
        RefBookRowValue[] firstVersionRows = objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002-data_version1.json"), "UTF-8"), RefBookRowValue[].class);
        RefBookRowValue[] secondVersionRows = objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002-data_version2.json"), "UTF-8"), RefBookRowValue[].class);

        VersionRestService versionService = mock(VersionRestService.class);

        when(versionService.search(anyString(), any(SearchDataCriteria.class))).thenAnswer(
                (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
                    SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
                    if (searchDataCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                    VersionMapping ek002VersionMapping = rdmSyncDao.getVersionMapping("EK002");
                    if(searchDataCriteria.getPageNumber() == 0 &&
                            (ek002VersionMapping == null || !"1".equals(ek002VersionMapping.getVersion())) ) {
                        return new RestPage<>(Arrays.asList(firstVersionRows));
                    } else if (searchDataCriteria.getPageNumber() == 0 && ek002VersionMapping != null || "1".equals(ek002VersionMapping.getVersion()))
                        return new RestPage<>(Arrays.asList(secondVersionRows));

                    return new RestPage<>(Collections.emptyList());
                });

        RefBookVersion ek002Version1 = new RefBookVersion();
        ek002Version1.setId(199);
        RefBookVersion ek002Version2 = new RefBookVersion();
        ek002Version2.setId(286);
        when(versionService.getVersion(eq("1"), eq("EK002"))).thenReturn(ek002Version1);
        when(versionService.getVersion(eq("2"), eq("EK002"))).thenReturn(ek002Version2);

        return versionService;
    }

    @Bean
    public CompareService compareService(){
        CompareService compareService = mock(CompareService.class);

        when(compareService.compareData(any(CompareDataCriteria.class))).thenAnswer(
                (Answer<RefBookDataDiff>) invocationOnMock -> {
                    CompareDataCriteria criteria = invocationOnMock.getArgument(0, CompareDataCriteria.class);
                    if(criteria.getPageNumber() == 0 && Integer.valueOf(199).equals(criteria.getOldVersionId()) && Integer.valueOf(286).equals(criteria.getNewVersionId())) {
                        return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream("/EK002_diff_v1_v2.json"), "UTF-8"), RefBookDataDiff.class);
                    }
                    return new RefBookDataDiff();

                });

        when(compareService.compareStructures(anyInt(), anyInt())).thenReturn(new StructureDiff());

        return compareService;

    }

}
