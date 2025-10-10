package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class UseCaseTest {

    private static MockServerClient mockServerClient;

    private static ClientAndServer clientAndServer;

    private static RdmLoggingService loggingService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RdmSyncDao rdmSyncDao;

    private final RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    private String baseUrl;



    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>
            ("postgres:15")
            .withDatabaseName("rdm_sync")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    public static void beforeAll() {
        postgres.start();
        clientAndServer = ClientAndServer.startClientAndServer();
        mockServerClient = new MockServerClient("localhost", clientAndServer.getPort());
        mockFnsi(mockServerClient);
        mockRdm(mockServerClient);
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        clientAndServer.stop();
    }

    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("rdm.backend.path", () -> "http://localhost:" + mockServerClient.getPort() + "/rdm");
        registry.add("rdm-sync.source.fnsi.values[0].url", () -> "http://localhost:" + mockServerClient.getPort() + "/fnsi");
//        хоть и остальные св-ва rdm-sync.source.fnsi.values[0].* не динамические, но приходиться их определять тут иначе
//        динамически объявленный url их зануляет почему то
        registry.add("rdm-sync.source.fnsi.values[0].userKey", () -> "test");
        registry.add("rdm-sync.source.fnsi.values[0].code", () -> "FNSI");
        registry.add("rdm-sync.source.fnsi.values[0].name", () -> "ФНСИ");
    }

    @BeforeEach
    void setUp() {
        loggingService = applicationContext.getBean("rdmLoggingService", RdmLoggingService.class);
        this.baseUrl = "http://localhost:" + port + "/api/rdm";
    }

    @Test
    void testFnsiSync() throws InterruptedException {
        testFnsiSync("1.2.643.5.1.13.2.1.1.725");
    }

    @Test
    void testFnsiSyncAutoCreatedOnXml() throws InterruptedException {
        testFnsiSync("1.2.643.5.1.13.2.1.1.726");
    }

    @Test
    void testDefaultValue() throws InterruptedException {

        final String oid = "1.2.643.5.1.13.13.11.1040";
        final String dataUrl = baseUrl + "/data/" + oid;

        // Загрузка начальной версии.
        final String startVersion = "1.0";
        ResponseEntity<String> startResponse = performRefBookSync(oid);
        assertSyncResponse(startResponse);
        waitVersionLoaded(oid, startVersion);

        final String v10Url = dataUrl + "/version/" + startVersion + "?getDeleted=false";
        final Map<String, Object> firstData = getDataByUrl(v10Url);
        assertEquals(3, getTotalElements(firstData));

        final List<Map<String, Object>> firstRows = getResultRows(firstData);
        firstRows.forEach(row -> Assertions.assertEquals("-1", row.get("src_id").toString()));

        // Загрузка следующей версии.
        final String nextVersion = "2.1";
        waitVersionLoaded(oid, nextVersion);

        final String v21Url = dataUrl + "/version/" + nextVersion + "?getDeleted=false";
        final Map<String, Object> nextData = getDataByUrl(v21Url);
        assertEquals(3, getTotalElements(nextData));

        final List<Map<String, Object>> nextRows = getResultRows(nextData);
        assertNotNull(nextRows);

        final Set<String> srcIds = nextRows.stream().map(row -> row.get("src_id").toString()).collect(toSet());
        Assertions.assertEquals(Set.of("1", "2", "3"), srcIds);
    }

    @Test
    void testLoadAndReadRdmRefBookAutoCreatedOnProperties() throws InterruptedException, JsonProcessingException {
        testLoadAndReadRdmNotVersionedRefBook("EK002");
    }

    @Test
    void testLoadAndReadRdmRefBookAutoCreatedOnXml() throws InterruptedException, JsonProcessingException {
        testLoadAndReadRdmNotVersionedRefBook("XML_EK002");
    }

    @Test
    void testRdmLoadSimpleVersionedAutoCreatedOnProperties() throws InterruptedException {
        testRdmLoadSimpleVersioned("EK003");
    }

    @Test
    void testRdmLoadSimpleVersionedAutoCreatedOnXml() throws InterruptedException {
        testRdmLoadSimpleVersioned("XML_EK003");
    }

    /**
     * Проверка работы с версионным справочником.
     *
     * @param refBookCode код справочника
     */
    private void testRdmLoadSimpleVersioned(String refBookCode) throws InterruptedException {

        final String dataUrl = baseUrl + "/data/" + refBookCode;

        // Загрузка начальной версии.
        final ResponseEntity<String> startResponse = performRefBookSync(refBookCode);
        assertSyncResponse(startResponse);
        String startVersion = "3.0";
        waitVersionLoaded(refBookCode, startVersion);

        final LoadedVersion actualLoadedVersion = rdmSyncDao.getActualLoadedVersion(refBookCode);
        assertNotNull(actualLoadedVersion);
        assertEquals(startVersion, actualLoadedVersion.getVersion());

        // Проверка записей начальной версии.
        final Map<String, Object> startData = getDataByUrl(dataUrl + "/version/" + startVersion);
        assertEquals(2, getTotalElements(startData));

        final List<Map<String, Object>> startRows = getResultRows(startData);
        startRows.forEach(this::prepareRowToAssert);
        Assertions.assertTrue(startRows.contains(Map.of("id", 9, "name", "Девять")));
        Assertions.assertTrue(startRows.contains(Map.of("id", 1, "name", "Один")));

        // Загрузка следующей версии.
        final ResponseEntity<String> nextResponse = performRefBookSync(refBookCode);
        assertSyncResponse(nextResponse);
        String nextVersion = "3.1";
        waitVersionLoaded(refBookCode, nextVersion);

        // Проверка записей следующей версии.
        final Map<String, Object> nextData = getDataByUrl(dataUrl + "/version/" + nextVersion);
        assertEquals(2, getTotalElements(nextData));

        final List<Map<String, Object>> nextRows = getResultRows(nextData);
        nextRows.forEach(this::prepareRowToAssert);
        Assertions.assertTrue(nextRows.contains(Map.of("id", 3, "name", "Три")));
        Assertions.assertTrue(nextRows.contains(Map.of("id", 2, "name", "Два")));
    }

    private List<Map<String, Object>> getResultRows(Map<String, Object> result) {

        assertNotNull(result);

        return (List<Map<String, Object>>) result.get("content");
    }

    private void testFnsiSync(String oid) throws InterruptedException {

        final String dataUrl = baseUrl + "/data/" + oid;

        // Загрузка начальной версии.
        final String startVersion = "1.2";
        final ResponseEntity<String> startResponse = performRefBookSync(oid);
        assertSyncResponse(startResponse);
        waitVersionLoaded(oid, startVersion);

        final LoadedVersion actualLoadedVersion = rdmSyncDao.getActualLoadedVersion(oid);
        assertNotNull(actualLoadedVersion);
        assertEquals(startVersion, actualLoadedVersion.getVersion());

        // Проверка записей начальной версии.
        final Map<String, Object> firstData = getDataByUrl(dataUrl + "?getDeleted=false");
        assertEquals(103, getTotalElements(firstData));

        final Map<String, Object> startRow1 = getDataByUrl(dataUrl + "/55");
        prepareRowToAssert(startRow1);
        assertEquals(
                Map.of("ID", 55, "MNN_ID", 18, "DRUG_FORM_ID", 23, "DOSE_ID", 20),
                startRow1
        );

        final Map<String, Object> startRow2 = getDataByUrl(dataUrl + "/103");
        prepareRowToAssert(startRow2);
        assertEquals(
                Map.of("ID", 103, "MNN_ID", 24, "DRUG_FORM_ID", 16, "DOSE_ID", 103),
                startRow2
        );

        // Загрузка следующей версии.
        final String nextVersion = "1.8";
        final ResponseEntity<String> nextResponse = performRefBookSync(oid);
        assertSyncResponse(nextResponse);
        waitVersionLoaded(oid, nextVersion);

        // Проверка записей следующей версии.
        final Map<String, Object> nextData = getDataByUrl(dataUrl + "?getDeleted=false");
        assertEquals(65, getTotalElements(nextData));

        final Map<String, Object> deletedData = getDataByUrl(dataUrl + "?getDeleted=true");
        assertEquals(47, getTotalElements(deletedData));

        // Удалённая запись.
        final Map<String, Object> deletedRow = getDataByUrl(dataUrl + "/103");
        prepareRowToAssert(deletedRow);
        assertEquals(
                Map.of("ID", 103, "MNN_ID", 24, "DRUG_FORM_ID", 16, "DOSE_ID", 103,
                        "deleted_ts", "2018-08-28T15:48:00"),
                deletedRow
        );

        // Изменённая запись.
        final Map<String, Object> changedRow = getDataByUrl(dataUrl + "/12");
        prepareRowToAssert(changedRow);
        assertEquals(
                Map.of("ID", 12, "MNN_ID", 7, "DRUG_FORM_ID", 1, "DOSE_ID", 12),
                changedRow
        );

        // Созданная запись.
        final Map<String, Object> createdRow = getDataByUrl(dataUrl + "/106");
        prepareRowToAssert(createdRow);
        assertEquals(
                Map.of("ID", 106, "MNN_ID", 25, "DRUG_FORM_ID", 16, "DOSE_ID", 30),
                createdRow
        );
    }

    /**
     * Проверка работы с неверсионным справочником.
     *
     * @param refBookCode код справочника
     */
    private void testLoadAndReadRdmNotVersionedRefBook(String refBookCode)
            throws InterruptedException, JsonProcessingException {

        final String dataUrl = baseUrl + "/data/" + refBookCode;
        final ObjectMapper dataToJsonMapper = new ObjectMapper();

        final String expectedStartData = "[" +
                "{\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false}," +
                "{\"ref\":\"tab\",\"name_ru\":\"Голубой_r\",\"code_en\":\"blue_\",\"id\":2,\"is_cold\":true}," +
                "{\"name_ru\":\"Фиолетовый\",\"id\":3,\"is_cold\":true}" +
                "]";
        final String expectedNextData = "[" +
                "{\"ref\":\"tab\",\"name_ru\":\"Голубой\",\"code_en\":\"blue\",\"id\":2,\"is_cold\":true}," +
                "{\"ref\":\"st\",\"name_ru\":\"желтый\",\"code_en\":\"yello\",\"id\":3,\"is_cold\":false}," +
                "{\"name_ru\":\"зеленый\",\"code_en\":\"green\",\"id\":4,\"is_cold\":false}" +
                "]";
        final String expectedDeletedData = "[" +
                "{\"deleted_ts\":\"2021-02-05T12:38:33\"," +
                "\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false}" +
                "]";

        // Загрузка начальной версии.
        final ResponseEntity<String> startResponse = performRefBookSync(refBookCode);
        assertSyncResponse(startResponse);
        String startVersion = "1";
        waitVersionLoaded(refBookCode, startVersion);

        final LoadedVersion actualLoadedVersion = rdmSyncDao.getActualLoadedVersion(refBookCode);
        assertNotNull(actualLoadedVersion);
        assertEquals(startVersion, actualLoadedVersion.getVersion());

        // Проверка записей начальной версии.
        final Map<String, Object> startData = getDataByUrl(dataUrl + "?getDeleted=false");
        assertEquals(3, getTotalElements(startData));

        final List<Map<String, Object>> startRows = getResultRows(startData);
        startRows.forEach(this::prepareRowToAssert);
        assertEquals(expectedStartData, dataToJsonMapper.writeValueAsString(startRows));

        // Загрузка следующей версии.
        final ResponseEntity<String> nextResponse = performRefBookSync(refBookCode);
        assertSyncResponse(nextResponse);
        String nextVersion = "2";
        waitVersionLoaded(refBookCode, nextVersion);

        // Проверка записей следующей версии.
        final Map<String, Object> nextData = getDataByUrl(dataUrl + "?getDeleted=false");
        assertEquals(3, getTotalElements(nextData));

        final List<Map<String, Object>> nextRows = getResultRows(nextData);
        nextRows.forEach(this::prepareRowToAssert);
        assertEquals(expectedNextData, new ObjectMapper().writeValueAsString(nextRows));

        // Удалённая запись.
        final Map<String, Object> deletedData = getDataByUrl(dataUrl + "?getDeleted=true");
        assertEquals(1, getTotalElements(deletedData));

        final List<Map<String, Object>> deletedRows = getResultRows(deletedData);
        deletedRows.forEach(this::prepareRowToAssert);
        assertEquals(expectedDeletedData, new ObjectMapper().writeValueAsString(deletedRows));
    }

    private int getTotalElements(Map<String, Object> result) {

        assertNotNull(result);

        return (int) result.get("totalElements");
    }

    /**
     * Ожидание загрузки версии справочника.
     *
     * @param code    код справочника
     * @param version ожидаемая версия
     */
    private void waitVersionLoaded(String code, String version) throws InterruptedException {

        for (int i = 0; i < 3 && !isVersionLoaded(code, version); i++) {
            Thread.sleep(2000);
        }

        boolean versionLoaded = isVersionLoaded(code, version);
        Assertions.assertTrue(versionLoaded, "expected version " + version + " was loaded");
    }

    protected boolean isVersionLoaded(String code, String version) {

        final LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(code, version);
        return loadedVersion != null && version.equals(loadedVersion.getVersion());
    }

    private void prepareRowToAssert(Map<String, Object> row) {
        assertNotNull(row);
        row.remove("_sync_rec_id");
    }

    private Map<String, Object> getDataByUrl(String dataUrl) {

        return restTemplate.exchange(
                dataUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }

    /**
     * Проверка статуса ответа при запросе на синхронизацию справочника.
     *
     * @param response ответ
     */
    private void assertSyncResponse(ResponseEntity<String> response) {

        assertNotNull(response);

        final HttpStatusCode statusCode = response.getStatusCode();
        assertNotNull(statusCode);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), statusCode.value());
    }

    /**
     * Отправка запроса на синхронизацию справочника.
     *
     * @param code код справочника
     * @return Ответ на запрос синхронизации
     */
    private ResponseEntity<String> performRefBookSync(String code) {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final String methodUrl = baseUrl + "/update/" + code;
        final HttpEntity<String> request = new HttpEntity<>("{}", headers);
        return restTemplate.postForEntity(methodUrl, request, String.class);
    }

    private static void mockRdm(MockServerClient client) {
        mockEK002(client);
        mockXML_EK002(client);
        mockEK003(client);
        mockXML_EK003(client);
    }

    private static void mockFnsi(MockServerClient client) {
        mock725_726(client, "1.2.643.5.1.13.2.1.1.725");
        mock725_726(client, "1.2.643.5.1.13.2.1.1.726");
        mock1040(client);
    }

    private static void mock725_726(MockServerClient client, String oid) {
        LocalDateTime pubDateV1_2 = LocalDateTime.of(2016, 12, 20, 0, 0);
        LocalDateTime pubDateV1_8 = LocalDateTime.of(2018, 8, 28, 15, 48);
        RefBookMock.instanceOf(client, () -> loggingService)
                .fnsi(oid)
                .withSearchRefBook("1.2", "1.2.643.5.1.13.2.1.1.725-refbook-1.2.json")
                .withSearchRefBook("1.8", "1.2.643.5.1.13.2.1.1.725-refbook-1.8.json")
                .withGetVersions("1.2.643.5.1.13.2.1.1.725_versions.json")
                .withGetPassport("1.2", "1.2.643.5.1.13.2.1.1.725_passport_v1.2.json")
                .withGetPassport("1.7", "1.2.643.5.1.13.2.1.1.725_passport_v1.2.json")
                .withGetPassport("1.8", "1.2.643.5.1.13.2.1.1.725_passport_v1.8.json")
                .withGetData("1.2", Map.of(
                        1, "1.2.643.5.1.13.2.1.1.725_data_v1.2_page1.json",
                        2, "1.2.643.5.1.13.2.1.1.725_data_v1.2_page2.json"))
                .withGetData("1.8", Map.of(1, "1.2.643.5.1.13.2.1.1.725_data_v1.8_page1.json"))
                .withCompare(pubDateV1_2, pubDateV1_8,
                        Map.of(
                                1, "1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page1.json",
                                2, "1.2.643.5.1.13.2.1.1.725_diff_v1.2_v1.8_page2.json"
                        )
                )
                .mock();

    }

    private static void mock1040(MockServerClient client) {
        String oid = "1.2.643.5.1.13.13.11.1040";
        LocalDateTime pubDateV1_2 =  LocalDateTime.of(2016, 12, 6, 0, 0);
        LocalDateTime pubDateV1_8 = LocalDateTime.of(2016, 12, 18, 0, 0);
        RefBookMock.instanceOf(client, () -> loggingService)
                .fnsi(oid)
                .withSearchRefBook("1.0", "1.2.643.5.1.13.13.11.1040_refbook_v1.0.json")
                .withSearchRefBook("2.1", "1.2.643.5.1.13.13.11.1040_refbook_v2.1.json")
                .withGetVersions("1.2.643.5.1.13.13.11.1040_versions.json")
                .withGetPassport("1.0", "1.2.643.5.1.13.13.11.1040_passport_v1.0.json")
                .withGetPassport("2.1", "1.2.643.5.1.13.13.11.1040_passport_v2.1.json")
                .withGetData("1.0", Map.of(
                        1, "1.2.643.5.1.13.13.11.1040_data_v1.0.json"))
                .withGetData("2.1", Map.of(1, "1.2.643.5.1.13.13.11.1040_data_v2.1.json"))
                .withCompare(pubDateV1_2, pubDateV1_8, Map.of(1, "1.2.643.5.1.13.13.11.1040_diff_v1.0_v2.1.json"))
                .mock();
    }

    private static void mockXML_EK003(MockServerClient client) {
        String refCode = "XML_EK003";
        RefBookMock.instanceOf(client, () -> loggingService)
                .rdm(refCode)
                .withGetByCodeAndVersion( "3.0", "XML_EK003_version_3.0.json")
                .withGetByCodeAndVersion( "3.1", "XML_EK003_version_3.1.json")
                .withVersions("XML_EK003_versions.json")
                .mock();
    }

    private static void mockEK002(MockServerClient client) {
        final String refCode = "EK002";
        int oldVersionId = 199;
        int newVersionId = 286;

        RefBookMock.instanceOf(client, () -> loggingService)
                .rdm(refCode)
                .withGetByCodeAndVersion( "1", "EK002_version_1.json")
                .withGetByCodeAndVersion( "2", "EK002_version_2.json")
                .withData(oldVersionId, Map.of(0, "EK002_version_1_data.json"))
                .withData(newVersionId, Map.of(0, "EK002_version_2_data.json"))
                .withStructureDiff(oldVersionId, newVersionId, "empty_structure_diff.json")
                .withDataDiff(oldVersionId, newVersionId, Map.of(0, "EK002_versions_data_diff.json"))
                .withVersions("EK002_versions.json")
                .mock();

    }

    private static void mockEK003(MockServerClient client) {
        final String refCode = "EK003";
        int oldVersionId = 206;
        String oldVersion = "3.0";
        int newVersionId = 293;
        String newVersion = "3.1";
        RefBookMock.instanceOf(client, () -> loggingService)
                .rdm(refCode)
                .withGetByCodeAndVersion(oldVersion, "EK003_version_3.0.json")
                .withGetByCodeAndVersion(newVersion, "EK003_version_3.1.json")
                .withData(oldVersionId, Map.of(0, "EK003_version_3.0_data.json"))
                .withData(newVersionId, Map.of(0, "EK003_version_3.1_data.json"))
                .withVersions("EK003_versions.json")
                .mock();
    }

    private static void mockXML_EK002(MockServerClient client) {
        final String refCode = "XML_EK002";
        String firstVersion = "1";
        String secondVersion = "2";

        RefBookMock.instanceOf(client, () -> loggingService)
                .rdm(refCode)
                .withGetByCodeAndVersion(firstVersion, "XML_EK002_version_1.json")
                .withGetByCodeAndVersion(secondVersion, "XML_EK002_version_2.json")
                .withVersions("XML_EK002_versions.json")
                .mock();
    }

}
