package ru.i_novus.ms.rdm.sync;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-fnsi-test.properties")
@Import(TestFnsiConfig.class)
@Testcontainers
public class FnsiSyncServiceUseCaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RdmSyncDao rdmSyncDao;

    private String baseUrl;

    private static final int MAX_TIMEOUT = 70;

    private static final String RECORD_SYS_COL = "_sync_rec_id";

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>
            ("postgres:15")
            .withDatabaseName("rdm_sync")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/rdm";
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void testFnsiSync() throws InterruptedException {
        testFnsiSync(TestFnsiConfig.OID);
    }

    @Test
    void testFnsiSyncAutoCreatedOnXml() throws InterruptedException {
        testFnsiSync(TestFnsiConfig.XML_OID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDefaultValue() throws InterruptedException {

        final String oid = "1.2.643.5.1.13.13.11.1040";
        ResponseEntity<String> startResponse = startSync(oid);
        assertStatusCodeIs204(startResponse);

        for (int i = 0; i < MAX_TIMEOUT && rdmSyncDao.getLoadedVersion(oid, "1.0") == null; i++) {
            Thread.sleep(1000);
        }

        final String v10Url = baseUrl + "/data/" + oid + "/version/1.0" + "?getDeleted=false";
        Map<String, Object> result = restTemplate.getForEntity(v10Url, Map.class).getBody();
        assertEquals(3, getTotalElements(result));

        List<Map<String, Object>> resultRows = getResultRows(result);
        resultRows.forEach( row -> Assertions.assertEquals("-1", row.get("src_id").toString()));

        // версия 2.1
        final String v21Url = baseUrl + "/data/" + oid + "/version/2.1" + "?getDeleted=false";
        result = restTemplate.getForEntity(v21Url, Map.class).getBody();
        assertEquals(3, getTotalElements(result));

        resultRows = getResultRows(result);
        assertNotNull(resultRows);

        final Set<String> srcIds = resultRows.stream().map(row -> row.get("src_id").toString()).collect(toSet());
        Assertions.assertEquals(Set.of("1", "2", "3"), srcIds);
    }

    @SuppressWarnings("unchecked")
    private void testFnsiSync(String oid) throws InterruptedException {

        final ResponseEntity<String> startResponse = startSync(oid);
        assertStatusCodeIs204(startResponse);

        for (int i = 0; i < MAX_TIMEOUT && !"1.2".equals(rdmSyncDao.getActualLoadedVersion(oid).getVersion()); i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = restTemplate.getForEntity(baseUrl + "/data/" + oid + "?getDeleted=false", Map.class).getBody();
        assertEquals(103, getTotalElements(result));

        Map<String, Object> resultByPk = restTemplate.getForEntity(baseUrl + "/data/" + oid + "/55", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        assertEquals(Map.of("ID", 55, "MNN_ID", 18, "DRUG_FORM_ID", 23, "DOSE_ID", 20),resultByPk);

        resultByPk = restTemplate.getForEntity(baseUrl + "/data/" + oid + "/103", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        assertEquals(Map.of("ID", 103, "MNN_ID", 24, "DRUG_FORM_ID", 16, "DOSE_ID", 103),resultByPk);

        //загрузка след версии
        final ResponseEntity<String> nextResponse = startSync(oid);
        assertStatusCodeIs204(nextResponse);

        for (int i = 0; i<MAX_TIMEOUT && !"1.8".equals(rdmSyncDao.getLoadedVersion(oid, "1.8").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = restTemplate.getForEntity(baseUrl + "/data/" + oid + "?getDeleted=false", Map.class).getBody();
        assertEquals(65, getTotalElements(result));

        Map<String, Object> deletedResult = restTemplate.getForEntity(baseUrl + "/data/" + oid + "?getDeleted=true", Map.class).getBody();
        assertEquals(47, getTotalElements(deletedResult));

        //удаленная
        resultByPk = restTemplate.getForEntity(baseUrl + "/data/" + oid + "/103", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        assertEquals(Map.of("ID", 103, "MNN_ID", 24, "DRUG_FORM_ID", 16, "DOSE_ID", 103, "deleted_ts", "2018-08-28T15:48:00"),resultByPk);

        //измененная
        resultByPk = restTemplate.getForEntity(baseUrl + "/data/" + oid + "/12", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        assertEquals(Map.of("ID", 12, "MNN_ID", 7, "DRUG_FORM_ID", 1, "DOSE_ID", 12),resultByPk);

        //новая
        resultByPk = restTemplate.getForEntity(baseUrl + "/data/" + oid + "/106", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        assertEquals(Map.of("ID", 106, "MNN_ID", 25, "DRUG_FORM_ID", 16, "DOSE_ID", 30),resultByPk);

        assertEquals(47, getTotalElements(deletedResult));
    }

    private static void assertStatusCodeIs204(ResponseEntity<String> response) {

        assertNotNull(response);

        final HttpStatusCode statusCode = response.getStatusCode();
        assertNotNull(statusCode);
        assertEquals(204, statusCode.value());
    }

    private void prepareRowToAssert(Map<String, Object> row) {

        assertNotNull(row);

        row.remove(RECORD_SYS_COL);
    }

    private int getTotalElements(Map<String, Object> result) {

        assertNotNull(result);

        return (int) result.get("totalElements");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResultRows(Map<String, Object> result) {

        assertNotNull(result);

        return (List<Map<String, Object>>) result.get("content");
    }

    private ResponseEntity<String> startSync(String refCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(baseUrl + "/update/" + refCode, new HttpEntity<>("{}", headers), String.class);
    }

    private Map<String, Object> getVersionedData(String refCode, String version) {
        return restTemplate.getForEntity(baseUrl + "/data/" + refCode +"/version/"+version, Map.class).getBody();
    }
}
