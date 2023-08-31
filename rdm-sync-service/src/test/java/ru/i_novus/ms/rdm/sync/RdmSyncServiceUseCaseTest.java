package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.OPENTABLE)
@Import(TestConfig.class)
public class RdmSyncServiceUseCaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RdmSyncDao rdmSyncDao;

    private String baseUrl;

    private static final int MAX_TIMEOUT = 70;

    private static final String RECORD_SYS_COL = "_sync_rec_id";

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/rdm";
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testLoadAndReadRefBookAutoCreatedOnProperties() throws InterruptedException, JsonProcessingException {
        testLoadAndReadNotVersionedRefBook(TestConfig.EK002);
    }

    @Test
    public void testLoadAndReadRefBookAutoCreatedOnXml() throws InterruptedException, JsonProcessingException {
        testLoadAndReadNotVersionedRefBook(TestConfig.XML_EK002);
    }

    private void testLoadAndReadNotVersionedRefBook(String refBookCode) throws InterruptedException, JsonProcessingException {
        String firstVersionActualData = "[{\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false},{\"ref\":\"tab\",\"name_ru\":\"Голубой_r\",\"code_en\":\"blue_\",\"id\":2,\"is_cold\":true},{\"name_ru\":\"Фиолетовый\",\"id\":3,\"is_cold\":true}]";
        String secondVersionActualData = "[{\"ref\":\"tab\",\"name_ru\":\"Голубой\",\"code_en\":\"blue\",\"id\":2,\"is_cold\":true},{\"ref\":\"st\",\"name_ru\":\"желтый\",\"code_en\":\"yello\",\"id\":3,\"is_cold\":false},{\"name_ru\":\"зеленый\",\"code_en\":\"green\",\"id\":4,\"is_cold\":false}]";
        String deletedActualData = "[{\"deleted_ts\":\"2021-02-05T12:38:33\",\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false}]";

        ResponseEntity<String> startResponse = startSync(refBookCode);
        assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"1".equals(rdmSyncDao.getLoadedVersion(refBookCode, "1").getVersion()); i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = restTemplate.getForEntity(baseUrl + "/data/" + refBookCode + "?getDeleted=false", Map.class).getBody();
        assertEquals(3, getTotalElements(result));
        List<Map<String, Object>> rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        assertEquals(firstVersionActualData, new ObjectMapper().writeValueAsString(rows));

        //загрузка след версии
        startResponse = startSync(refBookCode);
        assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"2".equals(rdmSyncDao.getLoadedVersion(refBookCode, "2").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = restTemplate.getForEntity(baseUrl + "/data/" + refBookCode + "?getDeleted=false", Map.class).getBody();
        Map<String, Object> deletedResult = restTemplate.getForEntity(baseUrl + "/data/" + refBookCode + "?getDeleted=true", Map.class).getBody();
        assertEquals(3, getTotalElements(result));
        rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        assertEquals(secondVersionActualData, new ObjectMapper().writeValueAsString(rows));

        assertEquals(1, getTotalElements(deletedResult));
        rows = getResultRows(deletedResult);
        rows.forEach(this::prepareRowToAssert);
        assertEquals(deletedActualData, new ObjectMapper().writeValueAsString(rows));
    }

    @Test
    public void testFnsiSync() throws InterruptedException {
        testFnsiSync(TestConfig.OID);
    }

    @Test
    public void testFnsiSyncAutoCreatedOnXml() throws InterruptedException {
        testFnsiSync(TestConfig.XML_OID);
    }

    private void testFnsiSync(String oid) throws InterruptedException {

        ResponseEntity<String> startResponse = startSync(oid);
        assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"1.2".equals(rdmSyncDao.getActualLoadedVersion(oid).getVersion()); i++) {
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
        startResponse = startSync(oid);
        assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"1.8".equals(rdmSyncDao.getLoadedVersion(oid, "1.8").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = restTemplate.getForEntity(baseUrl + "/data/" + oid + "?getDeleted=false", Map.class).getBody();
        Map<String, Object> deletedResult = restTemplate.getForEntity(baseUrl + "/data/" + oid + "?getDeleted=true", Map.class).getBody();
        assertEquals(65, getTotalElements(result));

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

    /**
     * Загрузка справочника c с версиями
     */
    @Test
    public void testRdmLoadSimpleVersionedAutoCreatedOnProperties() throws InterruptedException {
        testRdmLoadSimpleVersioned(TestConfig.EK003);
    }

    @Test
    public void testRdmLoadSimpleVersionedAutoCreatedOnXml() throws InterruptedException {
        testRdmLoadSimpleVersioned(TestConfig.XML_EK003);
    }

    private void testRdmLoadSimpleVersioned(String refBookCode) throws InterruptedException {
        ResponseEntity<String> startResponse = startSync(refBookCode);
        assertEquals(204, startResponse.getStatusCodeValue());


        for (int i = 0; i<MAX_TIMEOUT && !"3.0".equals(rdmSyncDao.getLoadedVersion(refBookCode, "3.0").getVersion()); i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = getVersionedData(refBookCode, "3.0");
        assertEquals(2, getTotalElements(result));
        List<Map<String, Object>> rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        assertTrue(rows.contains(Map.of("id", 9, "name", "Девять")));
        assertTrue(rows.contains(Map.of("id", 1, "name", "Один")));

        //загрузка след версии
        startResponse = startSync(refBookCode);
        assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"3.1".equals(rdmSyncDao.getLoadedVersion(refBookCode, "3.1").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = getVersionedData(refBookCode, "3.1");
        //Map<String, Object> deletedResult = getVersionedData(refBookCode, );
        assertEquals(2, getTotalElements(result));
        rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        assertTrue(rows.contains(Map.of("id", 3, "name", "Три")));
        assertTrue(rows.contains(Map.of("id", 2, "name", "Два")));

      /*  assertEquals(1, getTotalElements(deletedResult));
        rows = getResultRows(deletedResult);
        rows.forEach(this::prepareRowToAssert);
        assertEquals(deletedActualData, new ObjectMapper().writeValueAsString(rows));*/
    }

    private void prepareRowToAssert(Map<String, Object> row) {

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
