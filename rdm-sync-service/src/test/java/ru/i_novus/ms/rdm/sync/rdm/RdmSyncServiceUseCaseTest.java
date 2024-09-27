package ru.i_novus.ms.rdm.sync.rdm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.BaseSyncServiceUseCaseTest;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static ru.i_novus.ms.rdm.sync.rdm.TestRdmConfig.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-rdm-test.properties")
@Testcontainers
public class RdmSyncServiceUseCaseTest extends BaseSyncServiceUseCaseTest {

    @TestConfiguration
    private static class TestConfig extends TestRdmConfig {

        public TestConfig() {
            super();
        }
    }

    @Test
    void testLoadAndReadRefBookAutoCreatedOnProperties() throws InterruptedException, JsonProcessingException {
        testLoadAndReadNotVersionedRefBook(EK002);
    }

    @Test
    void testLoadAndReadRefBookAutoCreatedOnXml() throws InterruptedException, JsonProcessingException {
        testLoadAndReadNotVersionedRefBook(XML_EK002);
    }

    /**
     * Проверка работы с неверсионным справочником.
     *
     * @param refBookCode код справочника
     */
    private void testLoadAndReadNotVersionedRefBook(String refBookCode)
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
        waitVersionLoaded(refBookCode, EK002_START_VERSION);

        final LoadedVersion actualLoadedVersion = rdmSyncDao.getActualLoadedVersion(refBookCode);
        assertNotNull(actualLoadedVersion);
        assertEquals(EK002_START_VERSION, actualLoadedVersion.getVersion());

        // Проверка записей начальной версии.
        final Map<String, Object> startData = getDataByUrl(dataUrl + "?getDeleted=false");
        assertEquals(3, getTotalElements(startData));

        final List<Map<String, Object>> startRows = getResultRows(startData);
        startRows.forEach(this::prepareRowToAssert);
        assertEquals(expectedStartData, dataToJsonMapper.writeValueAsString(startRows));

        // Загрузка следующей версии.
        final ResponseEntity<String> nextResponse = performRefBookSync(refBookCode);
        assertSyncResponse(nextResponse);
        waitVersionLoaded(refBookCode, EK002_NEXT_VERSION);

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

    @Test
    void testRdmLoadSimpleVersionedAutoCreatedOnProperties() throws InterruptedException {
        testRdmLoadSimpleVersioned(EK003);
    }

    @Test
    void testRdmLoadSimpleVersionedAutoCreatedOnXml() throws InterruptedException {
        testRdmLoadSimpleVersioned(XML_EK003);
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
        waitVersionLoaded(refBookCode, EK003_START_VERSION);

        final LoadedVersion actualLoadedVersion = rdmSyncDao.getActualLoadedVersion(refBookCode);
        assertNotNull(actualLoadedVersion);
        assertEquals(EK003_START_VERSION, actualLoadedVersion.getVersion());

        // Проверка записей начальной версии.
        final Map<String, Object> startData = getDataByUrl(dataUrl + "/version/" + EK003_START_VERSION);
        assertEquals(2, getTotalElements(startData));

        final List<Map<String, Object>> startRows = getResultRows(startData);
        startRows.forEach(this::prepareRowToAssert);
        assertTrue(startRows.contains(Map.of("id", 9, "name", "Девять")));
        assertTrue(startRows.contains(Map.of("id", 1, "name", "Один")));

        // Загрузка следующей версии.
        final ResponseEntity<String> nextResponse = performRefBookSync(refBookCode);
        assertSyncResponse(nextResponse);
        waitVersionLoaded(refBookCode, EK003_NEXT_VERSION);

        // Проверка записей следующей версии.
        final Map<String, Object> nextData = getDataByUrl(dataUrl + "/version/" + EK003_NEXT_VERSION);
        assertEquals(2, getTotalElements(nextData));

        final List<Map<String, Object>> nextRows = getResultRows(nextData);
        nextRows.forEach(this::prepareRowToAssert);
        assertTrue(nextRows.contains(Map.of("id", 3, "name", "Три")));
        assertTrue(nextRows.contains(Map.of("id", 2, "name", "Два")));
    }

}
