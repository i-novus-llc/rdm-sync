package ru.i_novus.ms.fnsi.sync.impl;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.*;

class FnsiSyncSourceServiceTest {

    private final String url = "https://fnsi.mock.ru/port";

    private final String userKey = "test";

    private final RestTemplate restTemplate = new RestTemplate();

    private MockRestServiceServer mockServer;

    private final SyncSourceService syncSourceService = new FnsiSyncSourceService(restTemplate, url, userKey);

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    void testGetRefBook() throws URISyntaxException {

        String oid = "1.2.643.5.1.13.13.99.2.308";
        String version = "3.13";
        final RefBookStructure expectedStructure = new RefBookStructure(
                null,
                singletonList("ID"),
                Set.of(
                        new RefBookStructure.Attribute("ID", INTEGER, "Идентификатор"),
                        new RefBookStructure.Attribute("SMOCOD", STRING, "Код СМО в кодировке единого реестра СМО"),
                        new RefBookStructure.Attribute("CODPVP", STRING, "Порядковый номер пункта выдачи полисов"),
                        new RefBookStructure.Attribute("ADDRESS", STRING, "Фактический адрес пункта выдачи"),
                        new RefBookStructure.Attribute("PHONE", STRING, "Номер телефона пункта выдачи полисов"),
                        new RefBookStructure.Attribute("DATEEND", DATE, "Дата окончания действия записи"),
                        new RefBookStructure.Attribute("DATEBEG", DATE, "Дата начала действия записи")
                ));
        expectedStructure.setRefDescription("Реестр пунктов выдачи полисов (ФОМС)");
        searchRefBookMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_refbook.json"));
        passportMockServer(oid, version, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_passport.json"));

        final RefBookVersion refBook = syncSourceService.getRefBook(oid, null);
        assertEquals(oid, refBook.getCode());
        assertEquals("3.13", refBook.getVersion());
        assertEquals(LocalDateTime.of(2019, 10, 4, 17, 42), refBook.getFrom());
        assertEquals(expectedStructure, refBook.getStructure());
    }

    /**
     * получение не существующего справочника
     */
    @Test
    void testGetNotExistingRefBook() throws URISyntaxException {

        final String identifier = "not_exists_id";
        searchRefBookMockServer(identifier, new ClassPathResource("/fnsi_test_responses/not-found-ref.json"));
        assertNull(syncSourceService.getRefBook(identifier, null));

    }


    @Test
    void testGetData() throws URISyntaxException {

        String oid = "1.2.643.5.1.13.13.99.2.359";
        int pageSize = 170;
        searchRefBookMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_refbook.json"));
        passportMockServer(oid, "1.1", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_passport.json"));
        dataMockServer(oid, 1, pageSize, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_data_page_1.json"));
        dataMockServer(oid, 2, pageSize, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_data_page_2.json"));
        dataMockServer(oid, 3, pageSize, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_data_page_3.json"));

        final DataCriteria dataCriteria = new DataCriteria();
        dataCriteria.setCode(oid);
        dataCriteria.setPageSize(pageSize);
        final Set<String> fields = Set.of("ID", "NAME", "CODE", "RAZDEL", "DATE_BEGIN");
        dataCriteria.setFields(fields);
        dataCriteria.setRefBookStructure(
                new RefBookStructure(
                        emptyList(),
                        List.of("ID"),
                        Set.of(
                                new RefBookStructure.Attribute("ID", INTEGER, null),
                                new RefBookStructure.Attribute("NAME", STRING, null),
                                new RefBookStructure.Attribute("CODE", STRING, null),
                                new RefBookStructure.Attribute("RAZDEL", INTEGER, null),
                                new RefBookStructure.Attribute("DATE_BEGIN", DATE, null),
                                new RefBookStructure.Attribute("DATE_END", DATE, null)
                        )
                )
        );
        Page<Map<String, Object>> data = syncSourceService.getData(dataCriteria);

        assertEquals(174, data.getTotalElements());
        assertEquals(170, data.getContent().size());
        assertEquals(
                Map.of(
                        "ID", 1,
                        "NAME", "Микрохирургические, расширенные, комбинированные и реконструктивно-пластические операции на поджелудочной железе, в том числе лапароскопически ассистированные операции",
                        "CODE", "01.00.1.001",
                        "RAZDEL", 1,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(0)
        );
        assertEquals(
                Map.of(
                        "ID", 18,
                        "NAME", "Микрохирургические вмешательства при злокачественных (первичных и вторичных) и доброкачественных новообразованиях оболочек головного мозга с вовлечением синусов, серповидного отростка и намета мозжечка",
                        "CODE", "08.00.12.002",
                        "RAZDEL", 1,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(17)
        );
        //получение 2й страницы
        dataCriteria.setPageNumber(1);
        data = syncSourceService.getData(dataCriteria);
        assertEquals(174, data.getTotalElements());
        assertEquals(4, data.getContent().size());
        assertEquals(
                Map.of(
                        "ID", 171,
                        "NAME", "Хирургическая, сосудистая и эндоваскулярная реваскуляризация магистральных артерий нижних конечностей при синдроме диабетической стопы",
                        "RAZDEL", 2,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(0)
        );
        assertEquals(
                Map.of(
                        "ID", 174,
                        "NAME", "Гастроинтестинальные комбинированные рестриктивно-шунтирующие операции при сахарном диабете 2 типа",
                        "RAZDEL", 2,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(3)
        );

        // за 3й страницей
        dataCriteria.setPageNumber(2);
        data = syncSourceService.getData(dataCriteria);
        assertTrue(data.getContent().isEmpty());


        // проверка что исключенная колонка не будет в результате
        dataCriteria.setFields(Set.of("NAME", "CODE", "RAZDEL", "DATE_BEGIN"));
        dataCriteria.setPageNumber(1);
        data = syncSourceService.getData(dataCriteria);
        assertEquals(
                Map.of(
                        "NAME", "Хирургическая, сосудистая и эндоваскулярная реваскуляризация магистральных артерий нижних конечностей при синдроме диабетической стопы",
                        "RAZDEL", 2,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(0)
        );

    }


    @Test
    void testGetDiff() throws URISyntaxException {

        final RowDiff expected = new RowDiff(
                RowDiffStatusEnum.UPDATED,
                Map.of(
                        "ID", 1,
                        "SMOCOD", "01003",
                        "CODPVP", "001",
                        "ADDRESS", "Республика Адыгея, г. Майкоп, ул. Советская, 185",
                        "PHONE", "(8772) 59-32-00",
                        "DATEBEG", LocalDate.of(2017, 11, 21)
                ));
        String oid = "1.2.643.5.1.13.13.99.2.308";

        compareMockServer(
                oid,
                LocalDateTime.of(2019, 10, 3, 10, 19),
                LocalDateTime.of(2019, 10, 4, 17, 42),
                new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_v3.12_v3.13_diff.json")
        );
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_versions.json"));
        passportMockServer(oid, "3.13", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_passport.json"));

        final VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(
                oid,
                "3.13",
                "3.12",
                Set.of("ID", "SMOCOD", "CODPVP", "ADDRESS", "PHONE", "DATEBEG"),
                new RefBookStructure(
                        emptyList(),
                        List.of("ID"),
                        Set.of(
                                new RefBookStructure.Attribute("ID", INTEGER, "Идентификатор"),
                                new RefBookStructure.Attribute("SMOCOD", STRING, "Код СМО"),
                                new RefBookStructure.Attribute("CODPVP", STRING, "Код ПВП"),
                                new RefBookStructure.Attribute("ADDRESS", STRING, "Адрес"),
                                new RefBookStructure.Attribute("PHONE", STRING, "Номер телефона"),
                                new RefBookStructure.Attribute("DATEBEG", DATE, "Дата начала"),
                                new RefBookStructure.Attribute("DATEEND", DATE, "Дата конца")
                        )
                )
        );
        versionsDiffCriteria.setPageSize(200);
        VersionsDiff diff = syncSourceService.getDiff(versionsDiffCriteria);

        List<RowDiff> diffContent = diff.getRows().getContent();

        assertEquals(1, diffContent.size());
        assertEquals(expected, diffContent.get(0));
        assertFalse(diff.isStructureChanged());

        // Проверка использования только полей из критерия
        final VersionsDiffCriteria versionsDiffCriteria2 = new VersionsDiffCriteria(
                oid,
                "3.13",
                "3.12",
                Set.of("ID"),
                new RefBookStructure(
                        emptyList(),
                        List.of("ID"),
                        Set.of(
                                new RefBookStructure.Attribute("ID", INTEGER, "Идентификатор"),
                                new RefBookStructure.Attribute("SMOCOD", STRING, "Код СМО"),
                                new RefBookStructure.Attribute("CODPVP", STRING, "Код ПВП"),
                                new RefBookStructure.Attribute("ADDRESS", STRING, "Адрес"),
                                new RefBookStructure.Attribute("PHONE", STRING, "Номер телефона"),
                                new RefBookStructure.Attribute("DATEBEG", DATE, "Дата начала"),
                                new RefBookStructure.Attribute("DATEEND", DATE, "Дата конца")
                        )
                )
        );
        versionsDiffCriteria2.setPageSize(200);
        assertEquals(Set.of("ID"), syncSourceService.getDiff(versionsDiffCriteria2).getRows().getContent().get(0).getRow().keySet());

    }

    @Test
    void testGetDiffWithChangedStructure() throws URISyntaxException {

        String oid = "1.2.643.5.1.13.2.1.1.56";
        compareMockServer(
                oid,
                LocalDateTime.of(2016, 12, 29, 0, 0),
                LocalDateTime.of(2017, 1, 20, 0, 0),
                new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.56_v1.5_v1.6_diff.json")
        );
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.56_versions.json"));

        final VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(
                oid,
                "1.6",
                "1.5",
                Set.of("ID", "CODE", "NAME"),
                new RefBookStructure()
        );
        versionsDiffCriteria.setPageSize(200);
        assertTrue(syncSourceService.getDiff(versionsDiffCriteria).isStructureChanged());
    }

    @Test
    void testGetEmptyDiff() throws URISyntaxException {
        String oid = "1.2.643.5.1.13.2.1.1.56";
        compareMockServer(
                oid,
                LocalDateTime.of(2017, 1, 20, 0, 0),
                LocalDateTime.of(2017, 1, 20, 0, 1),
                new ClassPathResource("/fnsi_test_responses/empty_diff.json")
        );
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.56_versions.json"));

        final VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(
                oid,
                "1.7",
                "1.6",
                Set.of("ID", "CODE", "NAME"),
                new RefBookStructure()
        );
        versionsDiffCriteria.setPageSize(200);

        final VersionsDiff diff = syncSourceService.getDiff(versionsDiffCriteria);
        assertFalse(diff.isStructureChanged());
        assertTrue(diff.getRows().isEmpty());
    }

    @Test
    void testGetVersions() throws URISyntaxException {

        String oid = "1.2.643.5.1.13.2.1.1.725";
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.725_versions.json"));
        passportMockServer(oid, "1.7", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.7.json"));
        passportMockServer(oid, "1.8", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.8.json"));
        passportMockServer(oid, "1.9", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.725_passport_v1.9.json"));
        final List<RefBookVersionItem> versions = syncSourceService.getVersions(oid);

        final List<RefBookVersionItem> expected = List.of(
                new RefBookVersionItem(
                        oid,
                        "1.7",
                        LocalDateTime.of(2018, 7, 10, 0, 0),
                        LocalDateTime.of(2018, 8, 3, 0, 0),
                        null
                ),
                new RefBookVersionItem(
                        oid,
                        "1.8",
                        LocalDateTime.of(2018, 8, 3, 0, 0),
                        LocalDateTime.of(2018, 8, 29, 10, 54),
                        null
                ),
                new RefBookVersionItem(
                        oid,
                        "1.9",
                        LocalDateTime.of(2018, 8, 29, 10, 54),
                        null,
                        null
                )
        );

        assertEquals(expected, versions);

    }

    private void versionsMockServer(String identifier, Resource body) throws URISyntaxException {
        fnsiApiMockServer("/rest/versions", Map.of("identifier", identifier, "page", "1", "size", "200"), body);
    }

    private void compareMockServer(String identifier, LocalDateTime fromDate, LocalDateTime toDate, Resource body) throws URISyntaxException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<String, String> params = Map.of("identifier", identifier, "date1", encode(dtf.format(fromDate)), "date2", encode(dtf.format(toDate)), "page", "1", "size", "200");
        fnsiApiMockServer("/rest/compare", params, body);
    }

    private void searchRefBookMockServer(String identifier, Resource body) throws URISyntaxException {
        fnsiApiMockServer("/rest/searchDictionary", Map.of("identifier",identifier,"page", "1", "size", "200"), body);
    }

    private void passportMockServer(String identifier, String version, Resource body) throws URISyntaxException {
        fnsiApiMockServer("/rest/passport", Map.of("identifier", identifier, "version", version), body);
    }

    private void dataMockServer(String identifier, int page, int size, Resource body) throws URISyntaxException {
        fnsiApiMockServer("/rest/data", Map.of("identifier", identifier, "page", ""+page,"size", ""+size), body);
    }


    private void fnsiApiMockServer(String methodUrl, Map<String, String> params, Resource body) throws URISyntaxException {
        ResponseActions responseActions = mockServer.expect(ExpectedCount.manyTimes(),
                MockRestRequestMatchers.requestTo(Matchers.containsString(url + methodUrl)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.queryParam("userKey","test"));
        for(Map.Entry<String, String> entry : params.entrySet()){
            responseActions = responseActions.andExpect(MockRestRequestMatchers.queryParam(entry.getKey(), entry.getValue()));
        }
        responseActions
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                );

    }

    private String encode(String value) {
        return UriComponentsBuilder.fromPath((value)).toUriString();
    }
}
