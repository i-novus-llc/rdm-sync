package ru.i_novus.ms.rdm.sync.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:rdm-source-test.properties")
class RdmSyncSourceServiceTest {

    private static final String REF_BOOK_CODE = "A_REF";
    private static final String VERSION_1_0 = "1.0";
    private static final String VERSION_1_1 = "1.1";
    private static final int VERSION_1_0_ID = 110;
    private static final int VERSION_1_1_ID = 111;

    @Value("${rdm.backend.path}")
    private String baseUrl;

    private MockRestServiceServer mockServer;

    private SyncSourceService service;

    @BeforeEach
    public void init() {

        final RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);

        mockServer = MockRestServiceServer
                .bindTo(builder)
                .ignoreExpectOrder(true)
                .build();

        service = new RdmSyncSourceService(builder.build());
    }

    @AfterEach
    public void done() {
        mockServer.reset();
    }

    @Test
    void testGetRefBookByCodeAndVersion() {

        final String uri = "/version/{version}/refbook/{code}";
        final ResponseActions responseActions = mockRdmGet(uri, VERSION_1_1, REF_BOOK_CODE);
        mockRdmRespond(responseActions, "A_REF_version_1.1.json");

        final RefBookVersion version = service.getRefBook(REF_BOOK_CODE, VERSION_1_1);
        assertNotNull(version);
        assertEquals(REF_BOOK_CODE, version.getCode());
        assertEquals(VERSION_1_1, version.getVersion());
        assertStructure(version.getStructure());
    }

    @Test
    void testGetRefBookLastVersionByCode() {

        final String uri = "/version/refBook/{code}/last";
        final ResponseActions responseActions = mockRdmGet(uri, REF_BOOK_CODE);
        mockRdmRespond(responseActions, "A_REF_version_1.1.json");

        final RefBookVersion version = service.getRefBook(REF_BOOK_CODE, null);
        assertNotNull(version);
        assertEquals(REF_BOOK_CODE, version.getCode());
        assertEquals(VERSION_1_1, version.getVersion());
        assertStructure(version.getStructure());
    }

    @Test
    void testGetVersions() {

        final String uri = "/version/versions?refBookCode={code}";
        final ResponseActions responseActions = mockRdmGet(uri, REF_BOOK_CODE);
        mockRdmRespond(responseActions, "A_REF_versions.json");

        final List<RefBookVersionItem> versions = service.getVersions(REF_BOOK_CODE);
        assertNotNull(versions);
        assertEquals(2, versions.size());
        assertTrue(versions.stream().allMatch(RdmSyncSourceServiceTest::equalsRefBookCode));
    }

    private static boolean equalsRefBookCode(RefBookVersionItem version) {
        return version != null && REF_BOOK_CODE.equals(version.getCode());
    }

    @Test
    void testGetData() {

        final DataCriteria criteria = new DataCriteria();
        criteria.setCode(REF_BOOK_CODE);
        criteria.setVersion(VERSION_1_1);
        criteria.setVersionId(VERSION_1_1_ID);
        criteria.setFields(Set.of("id", "name"));
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        final String uri = "/version/{versionId}/data?page={page}&size={size}";
        final ResponseActions responseActions = mockRdmGet(uri,
                criteria.getVersionId(), criteria.getPageNumber(), criteria.getPageSize());
        mockRdmRespond(responseActions, "A_REF_version_1.1_data.json");

        final Page<Map<String, Object>> page = service.getData(criteria);
        assertNotNull(page);
        assertEquals(2L, page.getTotalElements());

        final List<Map<String, Object>> list = page.getContent();
        assertFalse(CollectionUtils.isEmpty(list));
        assertEquals(2, list.size());
        
        final Map<String, Object> item = list.get(0);
        assertFalse(CollectionUtils.isEmpty(item));
        assertEquals(2, item.size());
    }

    @Test
    void testGetDiff() {

        final VersionsDiffCriteria criteria = new VersionsDiffCriteria(
                REF_BOOK_CODE,
                VERSION_1_1,
                VERSION_1_0,
                Set.of("id", "name"),
                new RefBookStructure()
        );
        criteria.setOldVersionId(VERSION_1_0_ID);
        criteria.setNewVersionId(VERSION_1_1_ID);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        final String uriStructureDiff = "/compare/structures/{oldVersionId}-{newVersionId}";
        final ResponseActions responseStructureDiffActions = mockRdmGet(uriStructureDiff,
                criteria.getOldVersionId(), criteria.getNewVersionId());
        mockRdmRespond(responseStructureDiffActions, "A_REF_versions_structure_diff.json");

        final String uriDataDiff = "/compare/data?oldVersionId={oldVersionId}&newVersionId={newVersionId}" +
                "&page={page}&size={size}";
        final ResponseActions responseDataDiffActions = mockRdmGet(uriDataDiff,
                criteria.getOldVersionId(), criteria.getNewVersionId(),
                criteria.getPageNumber(), criteria.getPageSize());
        mockRdmRespond(responseDataDiffActions, "A_REF_versions_data_diff.json");

        final VersionsDiff versionsDiff = service.getDiff(criteria);
        assertNotNull(versionsDiff);
        assertFalse(versionsDiff.isStructureChanged());

        final Page<RowDiff> page = versionsDiff.getRows();
        assertNotNull(page);
        assertEquals(4L, page.getTotalElements());

        final List<RowDiff> list = page.getContent();
        assertFalse(CollectionUtils.isEmpty(list));
        assertEquals(4, list.size());

        final RowDiff rowDiff = list.get(0);
        assertNotNull(rowDiff);

        final Map<String, Object> item = rowDiff.getRow();
        assertFalse(CollectionUtils.isEmpty(item));
        assertEquals(2, item.size());
    }

    private ResponseActions mockRdmGet(String uri, Object... uriVars) {
        return mockServer
                .expect(MockRestRequestMatchers.requestToUriTemplate(baseUrl + uri, uriVars))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET));
    }

    private void mockRdmRespond(ResponseActions responseActions, String fileName) {

        final ClassPathResource body = new ClassPathResource("/responses/" + fileName);
        responseActions.andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );
    }

    private static void assertStructure(RefBookStructure structure) {

        assertNotNull(structure);
        assertEquals(1, structure.getReferences().size());
        assertEquals(1, structure.getPrimaries().size());
        assertEquals(4, structure.getAttributesAndTypes().size());
    }

}