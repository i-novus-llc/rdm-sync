package ru.i_novus.ms.rdm.sync.fnsi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.i_novus.ms.rdm.sync.BaseSyncServiceUseCaseTest;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.i_novus.ms.rdm.sync.fnsi.TestFnsiConfig.OID;
import static ru.i_novus.ms.rdm.sync.fnsi.TestFnsiConfig.XML_OID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestFnsiSyncApplication.class)
@Import(TestFnsiConfig.class)
@TestPropertySource("classpath:application-fnsi-test.properties")
@Testcontainers
public class FnsiSyncServiceUseCaseTest extends BaseSyncServiceUseCaseTest {

    @Test
    void testFnsiSync() throws InterruptedException {
        testFnsiSync(OID);
    }

    @Test
    void testFnsiSyncAutoCreatedOnXml() throws InterruptedException {
        testFnsiSync(XML_OID);
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

}
