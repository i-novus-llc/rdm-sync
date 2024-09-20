package ru.i_novus.ms.rdm.sync.fnsi;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.i_novus.ms.fnsi.sync.impl.FnsiSourceProperty;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TestFnsiConfig {

    public static final String OID = "1.2.643.5.1.13.2.1.1.725";
    public static final String XML_OID = "1.2.643.5.1.13.2.1.1.726";

    public static final String MOCK_USER_KEY = "mock";
    public static final String SERVICE_URL = "https://fnsi.mock.ru/port";

    private final FnsiSourceProperty property;

    @Autowired
    private RdmSyncDao rdmSyncDao;

    @Autowired
    private SyncSourceDao syncSourceDao;

    @Autowired
    private RdmLoggingService loggingService;

    public TestFnsiConfig(FnsiSourceProperty property) {
        this.property = property;
    }

    @Bean
    @Primary
    public RestTemplate fnsiRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SourceLoaderService mockFnsiSourceLoaderService() {
        return new MockFnsiSourceLoaderService(SERVICE_URL, MOCK_USER_KEY, syncSourceDao);
    }

    @Bean
    public SyncSourceServiceFactory mockFnsiSyncSourceServiceFactory(
            RestTemplate fnsiRestTemplate
    ) throws URISyntaxException {

        final MockRestServiceServer mockServer = MockRestServiceServer
                .bindTo(fnsiRestTemplate)
                .ignoreExpectOrder(true)
                .build();

        mockFnsi(mockServer, OID);
        mockFnsi(mockServer, XML_OID);

        final String refBookOid = "1.2.643.5.1.13.13.11.1040";
        mockSearchRefBook(mockServer, refBookOid, noVersionLoaded(refBookOid),
                "1.2.643.5.1.13.13.11.1040_refbook_v1.0.json");
        mockSearchRefBook(mockServer, refBookOid, versionLoaded(refBookOid, "1.0"),
                "1.2.643.5.1.13.13.11.1040_refbook_v2.1.json");
        mockGetVersions(mockServer, refBookOid,
                "1.2.643.5.1.13.13.11.1040_versions.json");
        mockGetPassport(mockServer, refBookOid, "1.0",
                "1.2.643.5.1.13.13.11.1040_passport_v1.0.json");
        mockGetPassport(mockServer, refBookOid, "2.1",
                "1.2.643.5.1.13.13.11.1040_passport_v2.1.json");
        mockGetData(mockServer,
                noVersionLoaded(refBookOid),
                refBookOid, 1, 100,
                "1.2.643.5.1.13.13.11.1040_data_v1.0.json");
        mockGetData(mockServer,
                noVersionLoaded(refBookOid),
                refBookOid, 2, 100,
                "1.2.643.5.1.13.13.11.1040_data_empty_page.json");
        mockGetData(mockServer,
                versionLoaded(refBookOid, "1.0"),
                refBookOid, 1, 100,
                "1.2.643.5.1.13.13.11.1040_data_v2.1.json");
        mockGetData(mockServer,
                versionLoaded(refBookOid, "1.0"),
                refBookOid, 2, 100,
                "1.2.643.5.1.13.13.11.1040_data_empty_page.json");
        mockCompare(mockServer,
                refBookOid,
                LocalDateTime.of(2016, 12, 6, 0, 0),
                LocalDateTime.of(2016, 12, 18, 0, 0),
                1,
                "1.2.643.5.1.13.13.11.1040_diff_v1.0_v2.1.json");

        return new MockFnsiSyncSourceServiceFactory(fnsiRestTemplate);
    }

    private void mockFnsi(MockRestServiceServer mockServer, String oid) throws URISyntaxException {

        // Эмуляция получения справочников в зависимости от того, что загружено
        mockSearchRefBook(mockServer, oid, noVersionLoaded(oid),
                "1.2.643.5.1.13.2.1.1.725-refbook-1.2.json");
        //searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  null),
        // "1.2.643.5.1.13.2.1.1.725-refbook-1.2.json");
        mockSearchRefBook(mockServer, oid, versionLoaded(oid,  "1.2"),
                "1.2.643.5.1.13.2.1.1.725-refbook-1.8.json");

        mockGetVersions(mockServer, oid,
                "1.2.643.5.1.13.2.1.1.725_versions.json");

        mockGetPassport(mockServer, oid, "1.2",
                "1.2.643.5.1.13.2.1.1.725_passport_v1.2.json");
        mockGetPassport(mockServer, oid, "1.7",
                "1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"); // Не важна какая структура
        mockGetPassport(mockServer, oid, "1.8",
                "1.2.643.5.1.13.2.1.1.725_passport_v1.8.json");

        mockGetData(mockServer, noVersionLoaded(oid), oid, 1, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.2_page1.json");
        mockGetData(mockServer, noVersionLoaded(oid), oid, 2, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.2_page2.json");
        mockGetData(mockServer, noVersionLoaded(oid), oid, 3, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.2_empty_page.json");
        mockGetData(mockServer, versionLoaded(oid, "1.2"), oid, 1, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.8_page1.json");
        mockGetData(mockServer, versionLoaded(oid, "1.2"), oid, 2, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.8_empty_page.json");

        mockCompare(mockServer, oid,
                LocalDateTime.of(2016, 12, 20, 0, 0),
                LocalDateTime.of(2018, 8, 28, 15, 48),
                1,
                "1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page1.json");
        mockCompare(mockServer, oid,
                LocalDateTime.of(2016, 12, 20, 0, 0),
                LocalDateTime.of(2018, 8, 28, 15, 48),
                2,
                "1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page2.json");

    }

    private void mockApi(
            MockRestServiceServer mockServer,
            RequestMatcher additionalMatcher,
            String methodUrl,
            Map<String, String> params,
            String fileName
    ) throws URISyntaxException {

        ResponseActions responseActions = mockServer
                .expect(
                        ExpectedCount.manyTimes(),
                        MockRestRequestMatchers.requestTo(
                                Matchers.containsString(SERVICE_URL + methodUrl)
                        ))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.queryParam("userKey", MOCK_USER_KEY));

        for(Map.Entry<String, String> entry : params.entrySet()) {
            responseActions = responseActions
                    .andExpect(MockRestRequestMatchers.queryParam(entry.getKey(), entry.getValue()));
        }

        if (additionalMatcher != null) {
            responseActions.andExpect(additionalMatcher);
        }

        final ClassPathResource body = new ClassPathResource("/fnsi_responses/" + fileName);
        responseActions.andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );

    }

    private void mockGetVersions(
            MockRestServiceServer mockServer,
            String identifier,
            String fileName
    ) throws URISyntaxException {

        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "page", "1",
                "size", "200"
        );
        mockApi(mockServer, null, "/rest/versions", params, fileName);
    }

    private void mockCompare(
            MockRestServiceServer mockServer,
            String identifier,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            String fileName
    ) throws URISyntaxException {

        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "date1", encode(dtf.format(fromDate)),
                "date2", encode(dtf.format(toDate)),
                "page", "" + page,
                "size", "100"
        );
        mockApi(mockServer, null, "/rest/compare", params, fileName);
    }

    private void mockSearchRefBook(
            MockRestServiceServer mockServer,
            String identifier,
            RequestMatcher requestMatcher,
            String fileName
    ) throws URISyntaxException {

        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "page", "1",
                "size", "200"
        );
        mockApi(mockServer, requestMatcher, "/rest/searchDictionary", params, fileName);
    }

    private void mockGetPassport(
            MockRestServiceServer mockServer,
            String identifier,
            String version,
            String fileName
    ) throws URISyntaxException {

        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "version", version
        );
        mockApi(mockServer, null, "/rest/passport", params, fileName);
    }

    private void mockGetData(
            MockRestServiceServer mockServer,
            RequestMatcher requestMatcher,
            String identifier,
            int page, int size,
            String fileName
    ) throws URISyntaxException {

        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "page", "" + page,
                "size", "" + size
        );
        mockApi(mockServer, requestMatcher, "/rest/data", params, fileName);
    }

    private String encode(String value) {
        return UriComponentsBuilder.fromPath((value)).toUriString();
    }

    /**
     * Проверка наличия загруженной версии справочника.
     *
     * @param oid     oid
     * @param version версия
     * @return Функция проверки
     */
    private RequestMatcher versionLoaded(String oid, String version) {
        return clientHttpRequest -> Assert.assertTrue(isVersionLoaded(oid, version));
    }

    private boolean isVersionLoaded(String oid, String version) {
        return loggingService.getList(LocalDate.now(), oid).stream()
                .anyMatch(log -> log.getNewVersion().equals(version));
    }

    /**
     * Проверка отсутствия загруженных версий справочника.
     *
     * @param oid oid
     * @return Функция проверки
     */
    private RequestMatcher noVersionLoaded(String oid) {
        return clientHttpRequest -> Assert.assertTrue(isNoVersionLoaded(oid));
    }

    private boolean isNoVersionLoaded(String oid) {
        return loggingService.getList(LocalDate.now(), oid).isEmpty();
    }

}
