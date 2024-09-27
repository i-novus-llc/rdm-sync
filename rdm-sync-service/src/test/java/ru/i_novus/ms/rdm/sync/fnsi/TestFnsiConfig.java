package ru.i_novus.ms.rdm.sync.fnsi;

import org.hamcrest.Matchers;
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
import ru.i_novus.ms.rdm.sync.TestBaseConfig;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TestFnsiConfig extends TestBaseConfig {

    public static final String OID = "1.2.643.5.1.13.2.1.1.725";
    public static final String XML_OID = "1.2.643.5.1.13.2.1.1.726";
    public static final String XML_DEF_VALUE_OID = "1.2.643.5.1.13.13.11.1040";

    public static final String MOCK_USER_KEY = "mock";
    public static final String SERVICE_URL = "https://fnsi.mock.ru/port";

    public TestFnsiConfig() {
        // Nothing to do.
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
    ) {
        final MockRestServiceServer mockServer = MockRestServiceServer
                .bindTo(fnsiRestTemplate)
                .ignoreExpectOrder(true)
                .build();

        mockFnsi(mockServer, OID);
        mockFnsi(mockServer, XML_OID);

        mockDefaultValue(mockServer);

        return new MockFnsiSyncSourceServiceFactory(fnsiRestTemplate);
    }

    private void mockFnsi(MockRestServiceServer mockServer, String oid) {

        // Эмуляция получения справочников в зависимости от того, что загружено
        mockSearchRefBook(mockServer, oid, checkNoneLoaded(oid),
                "1.2.643.5.1.13.2.1.1.725-refbook-1.2.json");
        //searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  null),
        // "1.2.643.5.1.13.2.1.1.725-refbook-1.2.json");
        mockSearchRefBook(mockServer, oid, checkGivenLoaded(oid,  "1.2"),
                "1.2.643.5.1.13.2.1.1.725-refbook-1.8.json");

        mockGetVersions(mockServer, oid,
                "1.2.643.5.1.13.2.1.1.725_versions.json");

        mockGetPassport(mockServer, oid, "1.2",
                "1.2.643.5.1.13.2.1.1.725_passport_v1.2.json");
        mockGetPassport(mockServer, oid, "1.7",
                "1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"); // Не важна какая структура
        mockGetPassport(mockServer, oid, "1.8",
                "1.2.643.5.1.13.2.1.1.725_passport_v1.8.json");

        mockGetData(mockServer, checkNoneLoaded(oid), oid, 1, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.2_page1.json");
        mockGetData(mockServer, checkNoneLoaded(oid), oid, 2, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.2_page2.json");
        mockGetData(mockServer, checkNoneLoaded(oid), oid, 3, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.2_empty_page.json");
        mockGetData(mockServer, checkGivenLoaded(oid, "1.2"), oid, 1, 100,
                "1.2.643.5.1.13.2.1.1.725_data_v1.8_page1.json");
        mockGetData(mockServer, checkGivenLoaded(oid, "1.2"), oid, 2, 100,
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

    private void mockDefaultValue(MockRestServiceServer mockServer) {

        final String refBookOid = XML_DEF_VALUE_OID;
        mockSearchRefBook(mockServer, refBookOid, checkNoneLoaded(refBookOid),
                "1.2.643.5.1.13.13.11.1040_refbook_v1.0.json");
        mockSearchRefBook(mockServer, refBookOid, checkGivenLoaded(refBookOid, "1.0"),
                "1.2.643.5.1.13.13.11.1040_refbook_v2.1.json");
        mockGetVersions(mockServer, refBookOid,
                "1.2.643.5.1.13.13.11.1040_versions.json");
        mockGetPassport(mockServer, refBookOid, "1.0",
                "1.2.643.5.1.13.13.11.1040_passport_v1.0.json");
        mockGetPassport(mockServer, refBookOid, "2.1",
                "1.2.643.5.1.13.13.11.1040_passport_v2.1.json");
        mockGetData(mockServer,
                checkNoneLoaded(refBookOid),
                refBookOid, 1, 100,
                "1.2.643.5.1.13.13.11.1040_data_v1.0.json");
        mockGetData(mockServer,
                checkNoneLoaded(refBookOid),
                refBookOid, 2, 100,
                "1.2.643.5.1.13.13.11.1040_data_empty_page.json");
        mockGetData(mockServer,
                checkGivenLoaded(refBookOid, "1.0"),
                refBookOid, 1, 100,
                "1.2.643.5.1.13.13.11.1040_data_v2.1.json");
        mockGetData(mockServer,
                checkGivenLoaded(refBookOid, "1.0"),
                refBookOid, 2, 100,
                "1.2.643.5.1.13.13.11.1040_data_empty_page.json");
        mockCompare(mockServer,
                refBookOid,
                LocalDateTime.of(2016, 12, 6, 0, 0),
                LocalDateTime.of(2016, 12, 18, 0, 0),
                1,
                "1.2.643.5.1.13.13.11.1040_diff_v1.0_v2.1.json");
    }

    private void mockGetVersions(
            MockRestServiceServer mockServer,
            String identifier,
            String fileName
    ) {

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
    ) {

        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "date1", valueToUri(dtf.format(fromDate)),
                "date2", valueToUri(dtf.format(toDate)),
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
    ) {

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
    ) {

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
    ) {

        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "page", "" + page,
                "size", "" + size
        );
        mockApi(mockServer, requestMatcher, "/rest/data", params, fileName);
    }

    private void mockApi(
            MockRestServiceServer mockServer,
            RequestMatcher additionalMatcher,
            String methodUrl,
            Map<String, String> params,
            String fileName
    ) {
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

}
