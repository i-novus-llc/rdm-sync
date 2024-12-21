package ru.i_novus.ms.rdm.sync.rdm;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.*;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.TestBaseConfig;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestRdmConfig extends TestBaseConfig {

    public static final String EK002 = "EK002";
    public static final String XML_EK002 = "XML_EK002";
    public static final String EK002_START_VERSION = "1";
    public static final String EK002_NEXT_VERSION = "2";
    public static final int EK002_START_VERSION_ID = 199;
    public static final int EK002_NEXT_VERSION_ID = 286;

    public static final String EK003 = "EK003";
    public static final String XML_EK003 = "XML_EK003";
    public static final String EK003_START_VERSION = "3.0";
    public static final String EK003_NEXT_VERSION = "3.1";
    public static final int EK003_START_VERSION_ID = 206;
    public static final int EK003_NEXT_VERSION_ID = 293;

    public static final String SERVICE_URL = "http://mock.rdm.api";
    private static final int MAX_DATA_SIZE = 100;

    public TestRdmConfig() {
        // Nothing to do.
    }

    @Bean
    private static MockServerRestClientCustomizer mockServerRestClientCustomizer() {
        return new MockServerRestClientCustomizer(UnorderedRequestExpectationManager.class);
    }

    @Bean
    @Primary
    public RestClient.Builder mockRdmRestClientBuilder(
            @Qualifier("mockServerRestClientCustomizer") MockServerRestClientCustomizer customizer
    ) {
        final RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);

        return builder.baseUrl(SERVICE_URL);
    }

    @Bean
    public SourceLoaderService mockRdmSourceLoaderService() {
        return new MockRdmSourceLoaderService(SERVICE_URL, syncSourceDao);
    }

    @Bean
    public SyncSourceServiceFactory mockRdmSyncSourceServiceFactory(
            @Qualifier("mockRdmRestClientBuilder") RestClient.Builder restClientBuilder,
            @Qualifier("mockServerRestClientCustomizer") MockServerRestClientCustomizer customizer
    ) {
        final MockRestServiceServer mockServer = customizer.getServer(restClientBuilder);

        mockRdm(mockServer);

        return new MockRdmSyncSourceServiceFactory(restClientBuilder);
    }

    private void mockRdm(MockRestServiceServer mockServer) {

        // EK002.
        mockByCodeAndVersion(mockServer, EK002, EK002_START_VERSION, "EK002_version_1.json", null);
        mockByCodeAndVersion(mockServer, EK002, EK002_NEXT_VERSION, "EK002_version_2.json", null);

        mockLastVersionByCode(mockServer, checkNoneLoaded(EK002),
                EK002, "EK002_version_1.json", null);
        mockLastVersionByCode(mockServer, checkGivenLoaded(EK002, EK002_START_VERSION),
                EK002, "EK002_version_2.json", null);

        mockData(mockServer, EK002_START_VERSION_ID, "EK002_version_1_data.json");
        mockData(mockServer, EK002_NEXT_VERSION_ID, "EK002_version_2_data.json");

        mockStructureDiff(mockServer, EK002_START_VERSION_ID, EK002_NEXT_VERSION_ID);
        mockDataDiff(mockServer, EK002_START_VERSION_ID, EK002_NEXT_VERSION_ID, "EK002_versions_data_diff.json");

        // XML_EK002.
        mockByCodeAndVersion(mockServer, XML_EK002, EK002_START_VERSION, "EK002_version_1.json", EK002);
        mockByCodeAndVersion(mockServer, XML_EK002, EK002_NEXT_VERSION, "EK002_version_2.json", EK002);

        mockLastVersionByCode(mockServer, checkNoneLoaded(XML_EK002),
                XML_EK002, "EK002_version_1.json", EK002);
        mockLastVersionByCode(mockServer, checkGivenLoaded(XML_EK002, EK002_START_VERSION),
                XML_EK002, "EK002_version_2.json", EK002);

        // EK003.
        mockByCodeAndVersion(mockServer, EK003, EK003_START_VERSION, "EK003_version_3.0.json", null);
        mockByCodeAndVersion(mockServer, EK003, EK003_NEXT_VERSION, "EK003_version_3.1.json", null);

        mockLastVersionByCode(mockServer, checkNoneLoaded(EK003),
                EK003, "EK003_version_3.0.json", null);
        mockLastVersionByCode(mockServer, checkGivenLoaded(EK003, EK003_START_VERSION),
                EK003, "EK003_version_3.1.json", null);

        mockData(mockServer, EK003_START_VERSION_ID, "EK003_version_3.0_data.json");
        mockData(mockServer, EK003_NEXT_VERSION_ID, "EK003_version_3.1_data.json");

        //mockStructureDiff(mockServer, EK003_START_VERSION_ID, EK003_NEXT_VERSION_ID);
        //mockDataDiff(mockServer, EK003_START_VERSION_ID, EK003_NEXT_VERSION_ID, "EK003_versions_data_diff.json");

        // XML_EK003.
        mockByCodeAndVersion(mockServer, XML_EK003, EK003_START_VERSION, "EK003_version_3.0.json", EK003);
        mockByCodeAndVersion(mockServer, XML_EK003, EK003_NEXT_VERSION, "EK003_version_3.1.json", EK003);

        mockLastVersionByCode(mockServer, checkNoneLoaded(XML_EK003),
                XML_EK003, "EK003_version_3.0.json", EK003);
        mockLastVersionByCode(mockServer, checkGivenLoaded(XML_EK003, EK003_START_VERSION),
                XML_EK003, "EK003_version_3.1.json", EK003);

    }

    private void mockByCodeAndVersion(MockRestServiceServer mockServer,
                                      String refBookCode, String version,
                                      String fileName, String replacedCode) {

        final String uri = "/version/{version}/refbook/{code}";
        final ResponseActions responseActions = mockRdmGet(mockServer, uri, version, refBookCode);
        mockRdmRespond(responseActions, fileName, refBookCode, replacedCode);
    }

    private void mockLastVersionByCode(MockRestServiceServer mockServer,
                                       RequestMatcher requestMatcher,
                                       String refBookCode,
                                       String fileName, String replacedCode) {

        final String uri = "/version/refBook/{code}/last";
        final ResponseActions responseActions = mockRdmGet(mockServer, uri, refBookCode);
        if (requestMatcher != null) {
            responseActions.andExpect(requestMatcher);
        }
        mockRdmRespond(responseActions, fileName, refBookCode, replacedCode);
    }

    private void mockData(MockRestServiceServer mockServer,
                          int versionId, String fileName) {

        final String uri = "/version/{versionId}/data?page={page}&size={size}";
        final ResponseActions responseActions1 = mockRdmGet(mockServer, uri, versionId, 0, MAX_DATA_SIZE);
        mockRdmRespond(responseActions1, fileName, null, null);

        final ResponseActions responseActions2 = mockRdmGet(mockServer, uri, versionId, 1, MAX_DATA_SIZE);
        mockRdmRespond(responseActions2, "empty_data.json", null, null);
    }

    private void mockStructureDiff(MockRestServiceServer mockServer,
                                   int oldVersionId, int newVersionId) {

        final String uri = "/compare/structures/{oldVersionId}-{newVersionId}";
        final ResponseActions responseActions = mockRdmGet(mockServer, uri, oldVersionId, newVersionId);
        mockRdmRespond(responseActions, "empty_structure_diff.json", null, null);
    }

    private void mockDataDiff(MockRestServiceServer mockServer,
                              int oldVersionId, int newVersionId,
                              String fileName) {

        final String uri = "/compare/data?oldVersionId={oldVersionId}&newVersionId={newVersionId}" +
                "&page={page}&size={size}";
        final ResponseActions responseActions1 = mockRdmGet(mockServer, uri, oldVersionId, newVersionId, 0, MAX_DATA_SIZE);
        mockRdmRespond(responseActions1, fileName, null, null);

        final ResponseActions responseActions2 = mockRdmGet(mockServer, uri, oldVersionId, newVersionId, 1, MAX_DATA_SIZE);
        mockRdmRespond(responseActions2, "empty_data_diff.json", null, null);
    }

    //private RefBookVersion getRefBookVersion(String path) throws IOException {
    //    return objectMapper.readValue(IOUtils.toString(TestRdmConfig.class.getResourceAsStream(path), "UTF-8"), RefBookVersion.class);
    //}
    //
    //private RefBookRowValue[] getVersionRows(String path) throws IOException {
    //    return objectMapper.readValue(IOUtils.toString(TestRdmConfig.class.getResourceAsStream(path), "UTF-8"), RefBookRowValue[].class);
    //}

    //private String encode(String value) {
    //    return UriComponentsBuilder.fromPath((value)).toUriString();
    //}

    private ResponseActions mockRdmGet(MockRestServiceServer mockServer, String uri, Object... uriVars) {
        return mockServer
                .expect(ExpectedCount.manyTimes(),
                        MockRestRequestMatchers.requestToUriTemplate(SERVICE_URL + uri, uriVars))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET));
    }

    private void mockRdmRespond(ResponseActions responseActions,
                                String fileName,
                                String refBookCode, String replacedCode) {

        final String filePath = "/rdm_responses/" + fileName;
        final Resource body = replacedCode == null
                ? new ClassPathResource(filePath)
                : new ByteArrayResource(
                        toCodeChangedBody(filePath, replacedCode, refBookCode).getBytes()
                );
        mockRdmRespond(responseActions, body);
    }

    private String toCodeChangedBody(String filePath, String refBookCode, String requiredCode) {

        if (StringUtils.isEmpty(filePath))
            throw new RuntimeException("A file path not specified");

        try {
            final InputStream stream = TestRdmConfig.class.getResourceAsStream(filePath);
            if (stream == null)
                throw new RuntimeException("A resource with file path '" + filePath + "' not specified");

            final String body = IOUtils.toString(stream, StandardCharsets.UTF_8);
            return body.replaceAll(refBookCode,requiredCode);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void mockRdmRespond(ResponseActions responseActions, Resource body) {

        responseActions.andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );
    }

}
