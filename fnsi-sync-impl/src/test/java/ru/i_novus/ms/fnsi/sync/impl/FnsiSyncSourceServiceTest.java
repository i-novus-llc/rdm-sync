package ru.i_novus.ms.fnsi.sync.impl;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

public class FnsiSyncSourceServiceTest {

    private final String url = "https://fnsi.mock.ru/port";

    private final String userKey = "test";

    private final RestTemplate restTemplate = new RestTemplate();

    private MockRestServiceServer mockServer;

    private final SyncSourceService syncSourceService = new FnsiSyncSourceService(restTemplate, url, userKey);

    @Before
    public void init() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    public void testGetRefBook() throws URISyntaxException {
        String oid = "1.2.643.5.1.13.13.99.2.308";
        String version = "3.13";
        RefBookStructure expectedStructure = new RefBookStructure(
                null,
                Collections.singletonList("ID"),
                Map.of(
                        "ID", AttributeTypeEnum.INTEGER,
                        "SMOCOD", AttributeTypeEnum.STRING,
                        "CODPVP", AttributeTypeEnum.STRING,
                        "ADDRESS", AttributeTypeEnum.STRING,
                        "PHONE", AttributeTypeEnum.STRING,
                        "DATEEND", AttributeTypeEnum.DATE,
                        "DATEBEG", AttributeTypeEnum.DATE));
        searchRefBookMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_refbook.json"));
        passportMockServer(oid, version, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_passport.json"));
        RefBook refBook = syncSourceService.getRefBook(oid);
        Assert.assertEquals(oid, refBook.getCode());
        Assert.assertEquals("3.13", refBook.getLastVersion());
        Assert.assertEquals(LocalDateTime.of(2019, 10, 4, 17, 42), refBook.getLastPublishDate());
        Assert.assertEquals(expectedStructure, refBook.getStructure());
    }

    /**
     * получение не существующего справочника
     */
    @Test
    public void testGetNotExistingRefBook() throws URISyntaxException {
        String identifier = "not_exists_id";
        searchRefBookMockServer(identifier, new ClassPathResource("/fnsi_test_responses/not-found-ref.json"));
        Assert.assertNull(syncSourceService.getRefBook(identifier));

    }


    @Test
    public void testGetData() throws URISyntaxException {
        String oid = "1.2.643.5.1.13.13.99.2.359";
        int pageSize = 170;
        searchRefBookMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_refbook.json"));
        passportMockServer(oid, "1.1", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_passport.json"));
        dataMockServer(oid, 1, pageSize, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_data_page_1.json"));
        dataMockServer(oid, 2, pageSize, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_data_page_2.json"));
        dataMockServer(oid, 3, pageSize, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.359_data_page_3.json"));

        DataCriteria dataCriteria = new DataCriteria();
        dataCriteria.setCode(oid);
        dataCriteria.setPageSize(pageSize);
        Page<Map<String, Object>> data = syncSourceService.getData(dataCriteria);

        Assert.assertEquals(174, data.getTotalElements());
        Assert.assertEquals(170, data.getContent().size());
        Assert.assertEquals(
                Map.of(
                        "ID", 1,
                        "NAME", "Микрохирургические, расширенные, комбинированные и реконструктивно-пластические операции на поджелудочной железе, в том числе лапароскопически ассистированные операции",
                        "CODE", "01.00.1.001",
                        "RAZDEL", 1,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(0)
        );
        Assert.assertEquals(
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
        Assert.assertEquals(174, data.getTotalElements());
        Assert.assertEquals(4, data.getContent().size());
        Assert.assertEquals(
                Map.of(
                        "ID", 171,
                        "NAME", "Хирургическая, сосудистая и эндоваскулярная реваскуляризация магистральных артерий нижних конечностей при синдроме диабетической стопы",
                        "RAZDEL", 2,
                        "DATE_BEGIN", LocalDate.of(2019, 1, 1)

                ),
                data.getContent().get(0)
        );
        Assert.assertEquals(
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
        Assert.assertTrue(data.getContent().isEmpty());
    }


    @Test
    public void testGetDiff() throws URISyntaxException {
        RowDiff expected = new RowDiff(
                RowDiffStatusEnum.UPDATED,
                Map.of(
                        "ID", 1,
                        "SMOCOD", "01003",
                        "CODPVP" , "001",
                        "ADDRESS", "Республика Адыгея, г. Майкоп, ул. Советская, 185",
                        "PHONE", "(8772) 59-32-00",
                        "DATEBEG", LocalDate.of(2017, 11, 21)
                )
        );
        String oid = "1.2.643.5.1.13.13.99.2.308";

        compareMockServer(
                oid,
                LocalDateTime.of(2019, 10, 3, 10, 19),
                LocalDateTime.of(2019, 10, 4, 17, 42),
                new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_v3.12_v3.13_diff.json")
        );
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_versions.json"));
        passportMockServer(oid, "3.13", new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.13.99.2.308_passport.json"));


        VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(oid, "3.13", "3.12");
        VersionsDiff diff = syncSourceService.getDiff(versionsDiffCriteria);


        List<RowDiff> diffContent = diff.getRows().getContent();

        Assert.assertEquals(1, diffContent.size());
        Assert.assertEquals(expected, diffContent.get(0));
        Assert.assertFalse(diff.isStructureChanged());
    }

    @Test
    public void testGetDiffWithChangedStructure() throws URISyntaxException {
        String oid = "1.2.643.5.1.13.2.1.1.56";
        compareMockServer(
                oid,
                LocalDateTime.of(2016, 12, 29, 0, 0),
                LocalDateTime.of(2017, 1, 20, 0, 0),
                new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.56_v1.5_v1.6_diff.json")
        );
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.56_versions.json"));
        VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(oid, "1.6", "1.5");
        Assert.assertTrue(syncSourceService.getDiff(versionsDiffCriteria).isStructureChanged());
    }

    @Test
    public void testGetEmptyDiff() throws URISyntaxException {
        String oid = "1.2.643.5.1.13.2.1.1.56";
        compareMockServer(
                oid,
                LocalDateTime.of(2017, 1, 20, 0, 0),
                LocalDateTime.of(2017, 1, 20, 0, 1),
                new ClassPathResource("/fnsi_test_responses/empty_diff.json")
        );
        versionsMockServer(oid, new ClassPathResource("/fnsi_test_responses/1.2.643.5.1.13.2.1.1.56_versions.json"));
        VersionsDiffCriteria versionsDiffCriteria = new VersionsDiffCriteria(oid, "1.7", "1.6");
        VersionsDiff diff = syncSourceService.getDiff(versionsDiffCriteria);
        Assert.assertFalse(diff.isStructureChanged());
        Assert.assertTrue(diff.getRows().isEmpty());
    }

    private void versionsMockServer(String identifier, Resource body) throws URISyntaxException {
        fnsiApiMockServer("/rest/versions", Map.of("identifier", identifier, "page", "1", "size", "200"), body);
    }

    private void compareMockServer(String identifier, LocalDateTime fromDate, LocalDateTime toDate, Resource body) throws URISyntaxException {
        DateTimeFormatter dtf =DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
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
