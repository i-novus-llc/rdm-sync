package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Value("${rdm_sync.fnsi.url:empty}")
    String fnsiUrl;

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

    @Bean
    public RestTemplate restTemplate() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        String oid ="1.2.643.5.1.13.2.1.1.725";
        //эмуляция получения справочников в зависимости от того что загружено
        searchRefBookMockServer(mockServer, oid, noVersionLoaded(oid), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.2.json"));
        searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  null), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.2.json"));
        searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  "1.2"), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.8.json"));

        versionsMockServer(mockServer, oid, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_versions.json"));

        passportMockServer(mockServer, oid, "1.2", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"));
        passportMockServer(mockServer, oid, "1.8", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.8.json"));

        dataMockServer(mockServer, versionLoaded(oid, null), oid, 1, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_page1.json"));
        dataMockServer(mockServer, versionLoaded(oid, null), oid, 2, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_page2.json"));
        dataMockServer(mockServer, versionLoaded(oid, null), oid, 3, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_empty_page.json"));
        dataMockServer(mockServer, versionLoaded(oid, "1.2"), oid, 1, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.8_page1.json"));
        dataMockServer(mockServer, versionLoaded(oid, "1.2"), oid, 2, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.8_empty_page.json"));

        compareMockServer(
                mockServer,
                oid,
                LocalDateTime.of(2016, 12, 20, 0, 0),
                LocalDateTime.of(2018, 8, 28, 15, 48),
                1,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page1.json"));
        compareMockServer(
                mockServer,
                oid,
                LocalDateTime.of(2016, 12, 20, 0, 0),
                LocalDateTime.of(2018, 8, 28, 15, 48),
                2,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page2.json"));
        return restTemplate;
    }

    private void fnsiApiMockServer(MockRestServiceServer mockServer, RequestMatcher additionalMatcher, String methodUrl, Map<String, String> params, ClassPathResource body) throws URISyntaxException {
        ResponseActions responseActions = mockServer.expect(ExpectedCount.manyTimes(),
                MockRestRequestMatchers.requestTo(Matchers.containsString(fnsiUrl + methodUrl)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.queryParam("userKey","test"));
        for(Map.Entry<String, String> entry : params.entrySet()){
            responseActions = responseActions.andExpect(MockRestRequestMatchers.queryParam(entry.getKey(), entry.getValue()));
        }
        if(additionalMatcher != null)
            responseActions.andExpect(additionalMatcher);
        responseActions
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                );

    }

    private void versionsMockServer(MockRestServiceServer mockServer, String identifier, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, null,"/rest/versions", Map.of("identifier", identifier, "page", "1", "size", "200"), body);
    }

    private void compareMockServer(MockRestServiceServer mockServer, String identifier, LocalDateTime fromDate, LocalDateTime toDate, int page,  ClassPathResource body) throws URISyntaxException {
        DateTimeFormatter dtf =DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<String, String> params = Map.of("identifier", identifier, "date1", encode(dtf.format(fromDate)), "date2", encode(dtf.format(toDate)), "page", ""+page, "size", "200");
        fnsiApiMockServer(mockServer, null, "/rest/compare", params, body);
    }

    private void searchRefBookMockServer(MockRestServiceServer mockServer, String identifier,  RequestMatcher requestMatcher, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, requestMatcher, "/rest/searchDictionary", Map.of("identifier",identifier,"page", "1", "size", "200"), body);
    }

    private void passportMockServer(MockRestServiceServer mockServer, String identifier, String version, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, null,"/rest/passport", Map.of("identifier", identifier, "version", version), body);
    }

    private void dataMockServer(MockRestServiceServer mockServer, RequestMatcher requestMatcher, String identifier, int page, int size, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, requestMatcher, "/rest/data", Map.of("identifier", identifier, "page", ""+page,"size", ""+size), body);
    }

    private String encode(String value) {
        return UriComponentsBuilder.fromPath((value)).toUriString();
    }

    /**
     * загружена ли версия
     * @param oid
     * @param version
     * @return
     */
    private RequestMatcher versionLoaded(String oid, String version) {
        return new RequestMatcher() {
            @Override
            public void match(ClientHttpRequest clientHttpRequest) throws IOException, AssertionError {
                VersionMapping versionMapping = rdmSyncDao.getVersionMapping(oid);
                if(version == null) {
                    Assert.assertNull(versionMapping.getVersion());
                } else {
                    Assert.assertEquals(version, versionMapping.getVersion());
                }
            }
        };
    }

    /**
     * ни одна версия не загружена
     * @param oid
     * @return
     */
    private RequestMatcher noVersionLoaded(String oid) {
        return new RequestMatcher() {
            @Override
            public void match(ClientHttpRequest clientHttpRequest) throws IOException, AssertionError {
                VersionMapping versionMapping = rdmSyncDao.getVersionMapping(oid);
                Assert.assertNull(versionMapping);
            }
        };
    }



}
