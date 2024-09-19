package ru.i_novus.ms.rdm.sync;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
import ru.i_novus.ms.fnsi.sync.impl.FnsiSyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@TestConfiguration
public class TestFnsiConfig {

    public static final String OID ="1.2.643.5.1.13.2.1.1.725";
    public static final String XML_OID ="1.2.643.5.1.13.2.1.1.726";

    private final FnsiSourceProperty property;

    @Autowired
    private RdmSyncDao rdmSyncDao;

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
    public SyncSourceServiceFactory syncSourceServiceFactory() throws URISyntaxException {

        final RestTemplate restTemplate = fnsiRestTemplate();

        final MockRestServiceServer mockServer = MockRestServiceServer
                .bindTo(restTemplate)
                .ignoreExpectOrder(true)
                .build();

        fnsiMock(mockServer, OID);
        fnsiMock(mockServer, XML_OID);

        final String refBookOid = "1.2.643.5.1.13.13.11.1040";
        searchRefBookMockServer(mockServer, refBookOid, noVersionLoaded(refBookOid),
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_refbook_v1.0.json"));
        searchRefBookMockServer(mockServer, refBookOid, versionLoaded(refBookOid, "1.0"),
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_refbook_v2.1.json"));
        versionsMockServer(mockServer, refBookOid,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_versions.json"));
        passportMockServer(mockServer, refBookOid, "1.0",
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_passport_v1.0.json"));
        passportMockServer(mockServer, refBookOid, "2.1",
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_passport_v2.1.json"));
        dataMockServer(mockServer,
                noVersionLoaded(refBookOid),
                refBookOid, 1, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_v1.0.json"));
        dataMockServer(mockServer,
                noVersionLoaded(refBookOid),
                refBookOid, 2, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_empty_page.json"));
        dataMockServer(mockServer,
                versionLoaded(refBookOid, "1.0"),
                refBookOid, 1, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_v2.1.json"));
        dataMockServer(mockServer,
                versionLoaded(refBookOid, "1.0"),
                refBookOid, 2, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_data_empty_page.json"));
        compareMockServer(mockServer,
                refBookOid,
                LocalDateTime.of(2016, 12, 6, 0, 0),
                LocalDateTime.of(2016, 12, 18, 0, 0),
                1,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.13.11.1040_diff_v1.0_v2.1.json"));

        return new FnsiSyncSourceServiceFactory(restTemplate);
    }

    private void fnsiMock(MockRestServiceServer mockServer, String oid) throws URISyntaxException {

        // Эмуляция получения справочников в зависимости от того что загружено
        searchRefBookMockServer(mockServer, oid, noVersionLoaded(oid),
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.2.json"));
        //searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  null),
        // new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.2.json"));
        searchRefBookMockServer(mockServer, oid, versionLoaded(oid,  "1.2"),
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725-refbook-1.8.json"));

        versionsMockServer(mockServer, oid,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_versions.json"));

        passportMockServer(mockServer, oid, "1.2",
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"));
        passportMockServer(mockServer, oid, "1.7",
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.2.json"));//не важна какая структура
        passportMockServer(mockServer, oid, "1.8",
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.8.json"));

        dataMockServer(mockServer, noVersionLoaded(oid), oid, 1, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_page1.json"));
        dataMockServer(mockServer, noVersionLoaded(oid), oid, 2, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_page2.json"));
        dataMockServer(mockServer, noVersionLoaded(oid), oid, 3, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.2_empty_page.json"));
        dataMockServer(mockServer, versionLoaded(oid, "1.2"), oid, 1, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.8_page1.json"));
        dataMockServer(mockServer, versionLoaded(oid, "1.2"), oid, 2, 100,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_data_v1.8_empty_page.json"));

        compareMockServer(mockServer, oid,
                LocalDateTime.of(2016, 12, 20, 0, 0),
                LocalDateTime.of(2018, 8, 28, 15, 48),
                1,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page1.json"));
        compareMockServer(mockServer, oid,
                LocalDateTime.of(2016, 12, 20, 0, 0),
                LocalDateTime.of(2018, 8, 28, 15, 48),
                2,
                new ClassPathResource("/fnsi_responses/1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page2.json"));

    }

    private void fnsiApiMockServer(
            MockRestServiceServer mockServer,
            RequestMatcher additionalMatcher,
            String methodUrl,
            Map<String, String> params,
            Resource body
    ) throws URISyntaxException {

        ResponseActions responseActions = mockServer
                .expect(
                        ExpectedCount.manyTimes(),
                        MockRestRequestMatchers.requestTo(
                                Matchers.containsString(property.getValues().get(0).getUrl() + methodUrl)
                        ))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.queryParam("userKey", "test"));

        for(Map.Entry<String, String> entry : params.entrySet()) {
            responseActions = responseActions
                    .andExpect(MockRestRequestMatchers.queryParam(entry.getKey(), entry.getValue()));
        }

        if (additionalMatcher != null) {
            responseActions.andExpect(additionalMatcher);
        }

        responseActions.andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );

    }

    private void versionsMockServer(
            MockRestServiceServer mockServer,
            String identifier,
            ClassPathResource body
    ) throws URISyntaxException {

        fnsiApiMockServer(
                mockServer,
                null,
                "/rest/versions",
                Map.of(
                        "identifier", identifier,
                        "page", "1",
                        "size", "200"
                ),
                body
        );
    }

    private void compareMockServer(
            MockRestServiceServer mockServer,
            String identifier,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            ClassPathResource body
    ) throws URISyntaxException {

        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final Map<String, String> params = Map.of(
                "identifier", identifier,
                "date1", encode(dtf.format(fromDate)),
                "date2", encode(dtf.format(toDate)),
                "page", "" + page,
                "size", "100"
        );
        fnsiApiMockServer(mockServer, null, "/rest/compare", params, body);
    }

    private void searchRefBookMockServer(
            MockRestServiceServer mockServer,
            String identifier,
            RequestMatcher requestMatcher,
            ClassPathResource body
    ) throws URISyntaxException {

        fnsiApiMockServer(
                mockServer,
                requestMatcher,
                "/rest/searchDictionary",
                Map.of(
                        "identifier",identifier,
                        "page", "1",
                        "size", "200"
                ),
                body
        );
    }

    private void passportMockServer(
            MockRestServiceServer mockServer,
            String identifier,
            String version,
            ClassPathResource body
    ) throws URISyntaxException {

        fnsiApiMockServer(
                mockServer,
                null,
                "/rest/passport",
                Map.of(
                        "identifier", identifier,
                        "version", version
                ),
                body
        );
    }

    private void dataMockServer(
            MockRestServiceServer mockServer,
            RequestMatcher requestMatcher,
            String identifier,
            int page, int size,
            Resource body
    ) throws URISyntaxException {

        fnsiApiMockServer(
                mockServer,
                requestMatcher,
                "/rest/data",
                Map.of(
                        "identifier", identifier,
                        "page", "" + page,
                        "size", "" + size
                ),
                body
        );
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
