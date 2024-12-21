package ru.i_novus.ms.rdm.sync;

import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract public class BaseSyncServiceUseCaseTest {

    protected static final int STATUS_CODE_NO_CONTENT = Response.Status.NO_CONTENT.getStatusCode();

    // Ожидание загрузки справочника:
    private static final int WAIT_LOAD_CHECK_SLEEP_TIME = 2000; // Длительность спящего режима потока (мс)
    //protected static final int WAIT_LOAD_CHECK_COUNT = 20; // Количество попыток проверки загруженности
    // Debug only
    protected static final int WAIT_LOAD_CHECK_COUNT = 3; // Количество попыток проверки загруженности

    protected static final String RECORD_SYS_COL = "_sync_rec_id";

    @LocalServerPort
    protected int port;

    @Autowired
    protected RdmSyncDao rdmSyncDao;

    @Autowired
    protected SyncSourceDao syncSourceDao;

    @Autowired
    protected RdmLoggingService loggingService;

    protected String baseUrl;

    protected final RestTemplate restTemplate = new RestTemplate();

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
        this.baseUrl = "http://localhost:" + port + "/api/rdm";
    }

    /**
     * Отправка запроса на синхронизацию справочника.
     *
     * @param code код справочника
     * @return Ответ на запрос синхронизации
     */
    protected ResponseEntity<String> performRefBookSync(String code) {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final String methodUrl = baseUrl + "/update/" + code;
        final HttpEntity<String> request = new HttpEntity<>("{}", headers);
        return restTemplate.postForEntity(methodUrl, request, String.class);
    }

    /**
     * Проверка статуса ответа при запросе на синхронизацию справочника.
     *
     * @param response ответ
     */
    protected void assertSyncResponse(ResponseEntity<String> response) {

        assertNotNull(response);

        final HttpStatusCode statusCode = response.getStatusCode();
        assertNotNull(statusCode);
        assertEquals(STATUS_CODE_NO_CONTENT, statusCode.value());
    }

    /**
     * Ожидание загрузки версии справочника.
     *
     * @param code    код справочника
     * @param version ожидаемая версия
     */
    protected void waitVersionLoaded(String code, String version) throws InterruptedException {

        for (int i = 0; i < WAIT_LOAD_CHECK_COUNT && !isVersionLoaded(code, version); i++) {
            Thread.sleep(WAIT_LOAD_CHECK_SLEEP_TIME);
        }

        boolean versionLoaded = isVersionLoaded(code, version);
        assertTrue(versionLoaded);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean isVersionLoaded(String code, String version) {

        final LoadedVersion loadedVersion = rdmSyncDao.getLoadedVersion(code, version);
        return loadedVersion != null && version.equals(loadedVersion.getVersion());
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getDataByUrl(String dataUrl) {

        return restTemplate.getForEntity(dataUrl, Map.class).getBody();
    }

    protected void prepareRowToAssert(Map<String, Object> row) {

        assertNotNull(row);

        row.remove(RECORD_SYS_COL);
    }

    protected int getTotalElements(Map<String, Object> result) {

        assertNotNull(result);

        return (int) result.get("totalElements");
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getResultRows(Map<String, Object> result) {

        assertNotNull(result);

        return (List<Map<String, Object>>) result.get("content");
    }

    /**
     * Преобразование значения в формат для подстановки в uri.
     *
     * @param value значение
     * @return Значение в формате для uri
     */
    protected static String valueToUri(String value) {
        return UriComponentsBuilder.fromPath((value)).toUriString();
    }

    /**
     * Проверка отсутствия загруженных версий справочника.
     *
     * @param code код справочника
     * @return Функция проверки
     */
    protected RequestMatcher checkNoneLoaded(String code) {
        return clientHttpRequest -> assertTrue(isNoneLoaded(code));
    }

    private boolean isNoneLoaded(String code) {
        return loggingService.getList(LocalDate.now(), code).isEmpty();
    }

    /**
     * Проверка наличия загруженной версии справочника.
     *
     * @param code    код справочника
     * @param version проверяемая версия
     * @return Функция проверки
     */
    protected RequestMatcher checkGivenLoaded(String code, String version) {
        return clientHttpRequest -> assertTrue(isGivenLoaded(code, version));
    }

    private boolean isGivenLoaded(String code, String version) {
        return loggingService.getList(LocalDate.now(), code).stream()
                .anyMatch(log -> log.getNewVersion().equals(version));
    }

}
