package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
import ru.i_novus.ms.fnsi.sync.impl.FnsiSourceProperty;
import ru.i_novus.ms.fnsi.sync.impl.FnsiSyncSourceServiceFactory;
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
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    public static final String EK002 = "EK002";
    public static final String XML_EK002 = "XMLEK002";
    public static final String EK003 = "EK003";
    public static final String XML_EK003 = "XMLEK003";
    public static final String OID ="1.2.643.5.1.13.2.1.1.725";
    public static final String XML_OID ="1.2.643.5.1.13.2.1.1.726";

    private final FnsiSourceProperty property;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Autowired
    private RdmLoggingService loggingService;

    public TestConfig(FnsiSourceProperty property) {
        this.property = property;
    }

    @PostConstruct
    public void  init() {
        objectMapper.addMixIn(RowValue.class, TestRowValueMixin.class);
    }

    @Bean
    public RefBookService refBookService() throws IOException {
        RefBookService refBookService = mock(RefBookService.class);
        RefBook ek002Ver1 = getRefBook("/EK002_version1.json");
        RefBook ek002Ver2 = getRefBook("/EK002_version2.json");
        RefBook xmlek002Ver1 = getRefBook("/EK002_version1.json", EK002, XML_EK002);
        RefBook xmlek002Ver2 = getRefBook("/EK002_version2.json", EK002, XML_EK002);

        RefBook ek003Ver3_0 = getRefBook("/rdm_responses/EK003_version_3.0.json");
        RefBook ek003Ver3_1 = getRefBook("/rdm_responses/EK003_version_3.1.json");
        RefBook xmlek003Ver3_0 = getRefBook("/rdm_responses/EK003_version_3.0.json", EK003, XML_EK003);
        RefBook xmlek003Ver3_1 = getRefBook("/rdm_responses/EK003_version_3.1.json", EK003, XML_EK003);

        when(refBookService.search(argThat(refBookCriteria -> refBookCriteria!=null && Set.of(EK002, XML_EK002).contains(refBookCriteria.getCodeExact()))))
                .thenAnswer((Answer<Page<RefBook>>) invocationOnMock -> {
                    RefBookCriteria refBookCriteria = invocationOnMock.getArgument(0, RefBookCriteria.class);
                    if (refBookCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                    boolean ver1IsLoaded = versionIsLoaded(refBookCriteria.getCodeExact(), "1");
                    if(!ver1IsLoaded) {
                        return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK002) ? ek002Ver1 : xmlek002Ver1));
                    } else
                        return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK002) ? ek002Ver2 : xmlek002Ver2));
                });
        when(refBookService.search(argThat(refBookCriteria -> Set.of(EK003, XML_EK003).contains(refBookCriteria.getCodeExact()))))
                .thenAnswer((Answer<Page<RefBook>>) invocationOnMock -> {
                    RefBookCriteria refBookCriteria = invocationOnMock.getArgument(0, RefBookCriteria.class);
                    if (refBookCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                    boolean ver3_0IsLoaded = versionIsLoaded(refBookCriteria.getCodeExact(), "3.0");
                    if(!ver3_0IsLoaded  ) {
                        return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK003) ? ek003Ver3_0 : xmlek003Ver3_0));
                    } else
                        return new RestPage<>(Collections.singletonList(refBookCriteria.getCodeExact().equals(EK003) ? ek003Ver3_1 : xmlek003Ver3_1));

                });
        return refBookService;
    }

    @Bean
    public VersionRestService versionService() throws IOException {
        RefBookRowValue[] ek002v1Rows = getVersionRows("/EK002-data_version1.json");
        RefBookRowValue[] ek002v2Rows = getVersionRows("/EK002-data_version2.json");
        RefBookRowValue[] ek003v3_0Rows = getVersionRows("/rdm_responses/EK003_data_version_3.0.json");
        RefBookRowValue[] ek003v3_1Rows = getVersionRows("/rdm_responses/EK003_data_version_3.1.json");

        VersionRestService versionService = mock(VersionRestService.class);

        RefBookVersion ek002Version1 = getRefBookVersion("/EK002_version1.json");
        RefBookVersion ek002Version2 = getRefBookVersion("/EK002_version2.json");
        RefBookVersion xmlEk002Version2 = getRefBookVersion("/EK002_version2.json");
        xmlEk002Version2.setCode(XML_EK002);
        RefBookVersion xmlEk002Version1 = getRefBookVersion("/EK002_version1.json");
        xmlEk002Version1.setCode(XML_EK002);
        when(versionService.getVersion(eq("1"), eq(EK002))).thenReturn(ek002Version1);
        when(versionService.getVersion(eq("2"), eq(EK002))).thenReturn(ek002Version2);
        when(versionService.getVersion(eq("1"), eq(XML_EK002))).thenReturn(xmlEk002Version1);
        when(versionService.getVersion(eq("2"), eq(XML_EK002))).thenReturn(xmlEk002Version2);
        RefBookVersion ek003Version3_0 = getRefBookVersion("/rdm_responses/EK003_version_3.0.json");
        RefBookVersion ek003Version3_1 = getRefBookVersion("/rdm_responses/EK003_version_3.1.json");
        RefBookVersion xmlEk003Version3_0 = getRefBookVersion("/rdm_responses/EK003_version_3.0.json");
        xmlEk003Version3_0.setCode(XML_EK003);
        RefBookVersion xmlEk003Version3_1 = getRefBookVersion("/rdm_responses/EK003_version_3.1.json");
        xmlEk003Version3_1.setCode(XML_EK003);
        when(versionService.getVersion(eq("3.0"), eq(EK003))).thenReturn(ek003Version3_0);
        when(versionService.getVersion(eq("3.1"), eq(EK003))).thenReturn(ek003Version3_1);
        when(versionService.getVersion(eq("3.0"), eq(XML_EK003))).thenReturn(xmlEk003Version3_0);
        when(versionService.getVersion(eq("3.1"), eq(XML_EK003))).thenReturn(xmlEk003Version3_1);
        when(versionService.search(eq(ek002Version1.getId()), any(SearchDataCriteria.class))).thenAnswer(
                (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
                    SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
                    if (searchDataCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                     if (searchDataCriteria.getPageNumber() == 0 )
                        return new RestPage<>(Arrays.asList(ek002v1Rows));

                    return new RestPage<>(Collections.emptyList());
                });

        when(versionService.search(eq(ek002Version2.getId()), any(SearchDataCriteria.class))).thenAnswer(
                (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
                    SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
                    if (searchDataCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                    if (searchDataCriteria.getPageNumber() == 0)
                        return new RestPage<>(Arrays.asList(ek002v2Rows));

                    return new RestPage<>(Collections.emptyList());
                });



        when(versionService.search(eq(ek003Version3_0.getId()), any(SearchDataCriteria.class))).thenAnswer(
                (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
                    SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
                    if (searchDataCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                    if (searchDataCriteria.getPageNumber() == 0)
                        return new RestPage<>(Arrays.asList(ek003v3_0Rows));

                    return new RestPage<>(Collections.emptyList());
                });

        when(versionService.search(eq(ek003Version3_1.getId()), any(SearchDataCriteria.class))).thenAnswer(
                (Answer<Page<RefBookRowValue>>) invocationOnMock -> {
                    SearchDataCriteria searchDataCriteria = invocationOnMock.getArgument(1, SearchDataCriteria.class);
                    if (searchDataCriteria.getPageNumber() >= 1) {
                        return new RestPage<>(Collections.emptyList());
                    }
                    if (searchDataCriteria.getPageNumber() == 0)
                        return new RestPage<>(Arrays.asList(ek003v3_1Rows));

                    return new RestPage<>(Collections.emptyList());
                });




        return versionService;
    }

    @Bean
    public CompareService compareService(){
        CompareService compareService = mock(CompareService.class);

        when(compareService.compareData(any(CompareDataCriteria.class))).thenAnswer(
                (Answer<RefBookDataDiff>) invocationOnMock -> {
                    CompareDataCriteria criteria = invocationOnMock.getArgument(0, CompareDataCriteria.class);
                    if(criteria.getPageNumber() == 0 && Integer.valueOf(199).equals(criteria.getOldVersionId()) && Integer.valueOf(286).equals(criteria.getNewVersionId())) {
                        return getRefBookDataDiff("/EK002_diff_v1_v2.json");
                    } else if (criteria.getPageNumber() == 0 && Integer.valueOf(206).equals(criteria.getOldVersionId()) && Integer.valueOf(293).equals(criteria.getNewVersionId())) {
                        return getRefBookDataDiff("/rdm_responses/EK003_diff_v3.0_v3.1.json");
                    }
                    return new RefBookDataDiff();

                });

        when(compareService.compareStructures(anyInt(), anyInt())).thenReturn(new StructureDiff());

        return compareService;

    }

    private RefBookDataDiff getRefBookDataDiff(String path) throws IOException {
        return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBookDataDiff.class);
    }

    @Bean
    public SyncSourceServiceFactory syncSourceServiceFactory() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        fnsiMock(mockServer, OID);
        fnsiMock(mockServer, XML_OID);
        //1.2.643.5.1.13.13.11.1040
        searchRefBookMockServer(mockServer, "1.2.643.5.1.13.13.11.1040", noVersionLoaded("1.2.643.5.1.13.13.11.1040"), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_refbook_v1.0.json"));
        searchRefBookMockServer(mockServer, "1.2.643.5.1.13.13.11.1040", versionLoaded("1.2.643.5.1.13.13.11.1040", "1.0"), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_refbook_v2.1.json"));
        versionsMockServer(mockServer, "1.2.643.5.1.13.13.11.1040", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_versions.json"));
        passportMockServer(mockServer, "1.2.643.5.1.13.13.11.1040", "1.0", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_passport_v1.0.json"));
        passportMockServer(mockServer, "1.2.643.5.1.13.13.11.1040", "2.1", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_passport_v2.1.json"));
        dataMockServer(mockServer, noVersionLoaded("1.2.643.5.1.13.13.11.1040"), "1.2.643.5.1.13.13.11.1040", 1, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_v1.0.json"));
        dataMockServer(mockServer, noVersionLoaded("1.2.643.5.1.13.13.11.1040"), "1.2.643.5.1.13.13.11.1040", 2, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_empty_page.json"));
        dataMockServer(mockServer, versionLoaded("1.2.643.5.1.13.13.11.1040", "1.0"), "1.2.643.5.1.13.13.11.1040", 1, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_v2.1.json"));
        dataMockServer(mockServer, versionLoaded("1.2.643.5.1.13.13.11.1040", "1.0"), "1.2.643.5.1.13.13.11.1040", 2, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_empty_page.json"));
        compareMockServer(
                mockServer,
                "1.2.643.5.1.13.13.11.1040",
                LocalDateTime.of(2016, 12, 6, 0, 0),
                LocalDateTime.of(2016, 12, 18, 0, 0),
                1,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_diff_v1.0_v2.1.json"));
        return new FnsiSyncSourceServiceFactory(restTemplate);
    }

    private void fnsiMock(MockRestServiceServer mockServer, String oid) throws URISyntaxException {
        //эмуляция получения справочников в зависимости от того что загружено
        searchRefBookMockServer(mockServer, oid, noVersionLoaded(oid), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.2.json"));
        //searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  null), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.2.json"));
        searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  "1.2"), new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.8.json"));

        versionsMockServer(mockServer, oid, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_versions.json"));

        passportMockServer(mockServer, oid, "1.2", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"));
        passportMockServer(mockServer, oid, "1.7", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"));//не важна какая структура
        passportMockServer(mockServer, oid, "1.8", new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.8.json"));


        dataMockServer(mockServer, noVersionLoaded(oid), oid, 1, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_page1.json"));
        dataMockServer(mockServer, noVersionLoaded(oid), oid, 2, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_page2.json"));
        dataMockServer(mockServer, noVersionLoaded(oid), oid, 3, 100, new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_empty_page.json"));
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



    }

    private void fnsiApiMockServer(MockRestServiceServer mockServer, RequestMatcher additionalMatcher, String methodUrl, Map<String, String> params, Resource body) throws URISyntaxException {
        ResponseActions responseActions = mockServer.expect(ExpectedCount.manyTimes(),
                MockRestRequestMatchers.requestTo(Matchers.containsString(property.getValues().get(0).getUrl() + methodUrl)))
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

    private RefBook getRefBook(String path) throws IOException {
        return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBook.class);
    }

    private RefBookVersion getRefBookVersion(String path) throws IOException {
        return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBookVersion.class);
    }

    private RefBook getRefBook(String path, String replacedCode, String code) throws IOException {
        String version = IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8").replaceAll(replacedCode, code);
        return objectMapper.readValue(version, RefBook.class);
    }

    private RefBookRowValue[] getVersionRows(String path) throws IOException {
        return objectMapper.readValue(IOUtils.toString(TestConfig.class.getResourceAsStream(path), "UTF-8"), RefBookRowValue[].class);
    }

    private void versionsMockServer(MockRestServiceServer mockServer, String identifier, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, null,"/rest/versions", Map.of("identifier", identifier, "page", "1", "size", "200"), body);
    }

    private void compareMockServer(MockRestServiceServer mockServer, String identifier, LocalDateTime fromDate, LocalDateTime toDate, int page,  ClassPathResource body) throws URISyntaxException {
        DateTimeFormatter dtf =DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<String, String> params = Map.of("identifier", identifier, "date1", encode(dtf.format(fromDate)), "date2", encode(dtf.format(toDate)), "page", ""+page, "size", "100");
        fnsiApiMockServer(mockServer, null, "/rest/compare", params, body);
    }

    private void searchRefBookMockServer(MockRestServiceServer mockServer, String identifier,  RequestMatcher requestMatcher, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, requestMatcher, "/rest/searchDictionary", Map.of("identifier",identifier,"page", "1", "size", "200"), body);
    }

    private void passportMockServer(MockRestServiceServer mockServer, String identifier, String version, ClassPathResource body) throws URISyntaxException {
        fnsiApiMockServer(mockServer, null,"/rest/passport", Map.of("identifier", identifier, "version", version), body);
    }

    private void dataMockServer(MockRestServiceServer mockServer, RequestMatcher requestMatcher, String identifier, int page, int size, Resource body) throws URISyntaxException {
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
                Assert.assertTrue(versionIsLoaded(oid, version));
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
            public void match(ClientHttpRequest clientHttpRequest) throws AssertionError {
                Assert.assertTrue(loggingService.getList(LocalDate.now(), oid).isEmpty());
            }
        };
    }

    private boolean versionIsLoaded(String code, String version) {
        return loggingService.getList(LocalDate.now() , code).stream().anyMatch(log -> log.getNewVersion().equals(version));
    }



}
