package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.Map;

import static ru.i_novus.ms.rdm.sync.dao.RdmSyncDaoImpl.RECORD_SYS_COL;

@RunWith(SpringRunner.class)
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

    private static final int MAX_TIMEOUT = 30;

    @Before
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/rdm";
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testLoadAndReadRefBook() throws InterruptedException, JsonProcessingException {
        String firstVersionActualData = "[{\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false},{\"ref\":\"tab\",\"name_ru\":\"Голубой_r\",\"code_en\":\"blue_\",\"id\":2,\"is_cold\":true},{\"name_ru\":\"Фиолетовый\",\"id\":3,\"is_cold\":true}]";
        String secondVersionActualData = "[{\"ref\":\"tab\",\"name_ru\":\"Голубой\",\"code_en\":\"blue\",\"id\":2,\"is_cold\":true},{\"ref\":\"st\",\"name_ru\":\"желтый\",\"code_en\":\"yello\",\"id\":3,\"is_cold\":false},{\"name_ru\":\"зеленый\",\"code_en\":\"green\",\"id\":4,\"is_cold\":false}]";
        String deletedActualData = "[{\"deleted_ts\":\"2021-02-05T12:38:33\",\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false}]";

        ResponseEntity<String> startResponse = startSync("EK002");
        Assert.assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"1".equals(rdmSyncDao.getLoadedVersion("EK002").getVersion()); i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = restTemplate.getForEntity(baseUrl + "/data/EK002?getDeleted=false", Map.class).getBody();
        Assert.assertEquals(3, getTotalElements(result));
        List<Map<String, Object>> rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        Assert.assertEquals(firstVersionActualData, new ObjectMapper().writeValueAsString(rows));

        //загрузка след версии
        startResponse = startSync("EK002");
        Assert.assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"2".equals(rdmSyncDao.getLoadedVersion("EK002").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = restTemplate.getForEntity(baseUrl + "/data/EK002?getDeleted=false", Map.class).getBody();
        Map<String, Object> deletedResult = restTemplate.getForEntity(baseUrl + "/data/EK002?getDeleted=true", Map.class).getBody();
        Assert.assertEquals(3, getTotalElements(result));
        rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        Assert.assertEquals(secondVersionActualData, new ObjectMapper().writeValueAsString(rows));

        Assert.assertEquals(1, getTotalElements(deletedResult));
        rows = getResultRows(deletedResult);
        rows.forEach(this::prepareRowToAssert);
        Assert.assertEquals(deletedActualData, new ObjectMapper().writeValueAsString(rows));
    }


    @Test
    public void testFnsiSync() throws InterruptedException {

        ResponseEntity<String> startResponse = startSync("1.2.643.5.1.13.2.1.1.725");
        Assert.assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"1.2".equals(rdmSyncDao.getLoadedVersion("1.2.643.5.1.13.2.1.1.725").getVersion()); i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725?getDeleted=false", Map.class).getBody();
        Assert.assertEquals(103, getTotalElements(result));
        Map<String, Object> resultByPk = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725/55", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        Assert.assertEquals(Map.of("ID", 55, "MNN_ID", 18, "DRUG_FORM_ID", 23, "DOSE_ID", 20),resultByPk);

        resultByPk = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725/103", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        Assert.assertEquals(Map.of("ID", 103, "MNN_ID", 24, "DRUG_FORM_ID", 16, "DOSE_ID", 103),resultByPk);

        //загрузка след версии
        startResponse = startSync("1.2.643.5.1.13.2.1.1.725");
        Assert.assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"1.8".equals(rdmSyncDao.getLoadedVersion("1.2.643.5.1.13.2.1.1.725").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725?getDeleted=false", Map.class).getBody();
        Map<String, Object> deletedResult = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725?getDeleted=true", Map.class).getBody();
        Assert.assertEquals(65, getTotalElements(result));

        //удаленная
        resultByPk = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725/103", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        Assert.assertEquals(Map.of("ID", 103, "MNN_ID", 24, "DRUG_FORM_ID", 16, "DOSE_ID", 103, "deleted_ts", "2018-08-28T15:48:00"),resultByPk);

        //измененная
        resultByPk = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725/12", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        Assert.assertEquals(Map.of("ID", 12, "MNN_ID", 7, "DRUG_FORM_ID", 1, "DOSE_ID", 12),resultByPk);


        //новая
        resultByPk = restTemplate.getForEntity(baseUrl + "/data/1.2.643.5.1.13.2.1.1.725/106", Map.class).getBody();
        prepareRowToAssert(resultByPk);
        Assert.assertEquals(Map.of("ID", 106, "MNN_ID", 25, "DRUG_FORM_ID", 16, "DOSE_ID", 30),resultByPk);

        Assert.assertEquals(47, getTotalElements(deletedResult));
    }

    /**
     * Загрузка справочника c с версиями
     */
    @Test
    public void testRdmLoadSimpleVersioned() throws InterruptedException {

        String refBookCode = "EK003";
        ResponseEntity<String> startResponse = startSync(refBookCode);
        Assert.assertEquals(204, startResponse.getStatusCodeValue());


        for (int i = 0; i<MAX_TIMEOUT && !"3.0".equals(rdmSyncDao.getLoadedVersion(refBookCode).getVersion()); i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = getVersionedData(refBookCode, "3.0");
        Assert.assertEquals(2, getTotalElements(result));
        List<Map<String, Object>> rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        Assert.assertTrue(rows.contains(Map.of("id", 9, "name", "Девять")));
        Assert.assertTrue(rows.contains(Map.of("id", 1, "name", "Один")));

        //загрузка след версии
        startResponse = startSync(refBookCode);
        Assert.assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<MAX_TIMEOUT && !"3.1".equals(rdmSyncDao.getLoadedVersion("EK003").getVersion()); i++) {
            Thread.sleep(1000);
        }
        result = getVersionedData(refBookCode, "3.1");
        //Map<String, Object> deletedResult = getVersionedData(refBookCode, );
        Assert.assertEquals(2, getTotalElements(result));
        rows = getResultRows(result);
        rows.forEach(this::prepareRowToAssert);
        Assert.assertTrue(rows.contains(Map.of("id", 3, "name", "Три")));
        Assert.assertTrue(rows.contains(Map.of("id", 2, "name", "Два")));

      /*  Assert.assertEquals(1, getTotalElements(deletedResult));
        rows = getResultRows(deletedResult);
        rows.forEach(this::prepareRowToAssert);
        Assert.assertEquals(deletedActualData, new ObjectMapper().writeValueAsString(rows));*/
    }

    private void prepareRowToAssert(Map<String, Object> row) {

        row.remove(RECORD_SYS_COL);
    }

    private int getTotalElements(Map<String, Object> result) {

        Assert.assertNotNull(result);

        return (int) result.get("totalElements");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getResultRows(Map<String, Object> result) {

        Assert.assertNotNull(result);

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
