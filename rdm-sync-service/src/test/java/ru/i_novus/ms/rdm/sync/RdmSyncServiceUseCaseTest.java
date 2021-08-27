package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Import(TestConfig.class)
public class RdmSyncServiceUseCaseTest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @Before
    public void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/api/rdm";
    }

    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testLoadAndReadRefBook() throws InterruptedException, JsonProcessingException {
        String actualData = "[{\"name_ru\":\"Красный\",\"code_en\":\"red\",\"id\":1,\"is_cold\":false},{\"ref\":\"tab\",\"name_ru\":\"Голубой_r\",\"code_en\":\"blue_\",\"id\":2,\"is_cold\":true},{\"name_ru\":\"Фиолетовый\",\"id\":3,\"is_cold\":true}]";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> startResponse = restTemplate.postForEntity(baseUrl + "/update/EK002", new HttpEntity<>("{}", headers), String.class);
        Assert.assertEquals(204, startResponse.getStatusCodeValue());

        for (int i = 0; i<3; i++) {
            Thread.sleep(1000);
        }
        Map<String, Object> result = restTemplate.getForEntity(baseUrl + "/data/EK002?getDeleted=false", Map.class).getBody();
        Assert.assertEquals(3, result.get("totalElements"));
        Assert.assertEquals(actualData, new ObjectMapper().writeValueAsString(result.get("content")));

    }
}
