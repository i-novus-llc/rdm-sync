package ru.i_novus.ms.rdm.sync.service.downloader;


import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmMappingServiceImpl;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefBookDownloaderImplTest {


    private RefBookDownloaderImpl refBookDownloader;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private SyncSourceService syncSourceService;

    @BeforeEach
    public void setUp() throws Exception {
        refBookDownloader = new RefBookDownloaderImpl(syncSourceService, dao, new RdmMappingServiceImpl(), 1, 0, 2);

        when(dao.getFieldMappings(anyInt())).thenReturn(List.of(
                new FieldMapping("ID", "integer", "ID"),
                new FieldMapping("CODE", "varchar", "CODE"),
                new FieldMapping("name", "varchar", "name")
        ));
        when(syncSourceService.getRefBook(anyString(), anyString())).thenAnswer(invocation -> {
            RefBookVersion refBookVersion = new RefBookVersion();
            refBookVersion.setVersion(invocation.getArgument(1));
            refBookVersion.setCode(invocation.getArgument(0));
            refBookVersion.setStructure(new RefBookStructure(null, Collections.singletonList("ID"),
                    Map.of("ID", AttributeTypeEnum.INTEGER, "CODE", AttributeTypeEnum.STRING, "name", AttributeTypeEnum.STRING)));
            return refBookVersion;
        });
    }

    /**
     * Скачивание версии справочника целиком
     */
    @Test
    public void testDowloadVersion() {
        Page<Map<String, Object>> page1 = pageOf(
                Map.of("ID", BigInteger.valueOf(1), "CODE", "code1", "name", "name1"),
                Map.of("ID", BigInteger.valueOf(2), "CODE", "code2", "name", "name2")

        );
        Page<Map<String, Object>> page2 = pageOf(
                Map.of("ID", BigInteger.valueOf(3), "CODE", "code3", "name", "name3"),
                Map.of("ID", BigInteger.valueOf(4), "CODE", "code4", "name", "name4")

        );
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 0)))
                .thenReturn(page1);

        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 1)))
                .thenReturn(page2);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() > 1)))
                .thenReturn(Page.empty());
        String refBookTable = "ref_table";
        when(dao.getVersionMapping(anyString(), anyString()))
                .thenReturn(VersionMapping.builder()
                        .table(refBookTable)
                        .primaryField("id")
                        .id(1)
                        .build()
                );

        DownloadResult downloadResult = refBookDownloader.download("code", "1.0");
        Assert.assertEquals(DownloadResultType.VERSION, downloadResult.getType());
        verify(dao, times(1)).createVersionTempDataTbl("temp_code_1_0", refBookTable, "_sync_rec_id","id");
        ArgumentCaptor<List<Map<String, Object>>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(dao, times(2)).insertVersionAsTempData(eq("temp_code_1_0"), dataCaptor.capture());
        Assert.assertEquals(2, dataCaptor.getAllValues().size());
        Assert.assertEquals(page1.getContent(), dataCaptor.getAllValues().get(0));
        Assert.assertEquals(page2.getContent(), dataCaptor.getAllValues().get(1));
    }

    /**
     * Проверка загрузки изменений между версиями
     */
    @Test
    public void testDownloadDiff() {
        RowDiff rec1 = new RowDiff(RowDiffStatusEnum.UPDATED, Map.of("ID", BigInteger.valueOf(1), "CODE", "code", "name", "testName"));
        RowDiff rec2 = new RowDiff(RowDiffStatusEnum.INSERTED, Map.of("ID", BigInteger.valueOf(2), "CODE", "code2", "name", "testName2"));
        RowDiff rec3 = new RowDiff(RowDiffStatusEnum.INSERTED, Map.of("ID", BigInteger.valueOf(3), "CODE", "code3", "name", "testName3"));
        RowDiff rec0 = new RowDiff(RowDiffStatusEnum.DELETED, Map.of("ID", BigInteger.valueOf(0), "CODE", "code0", "name", "testName0"));
        when(dao.getLoadedVersion(anyString(), anyString())).thenReturn(null);
        when(dao.existsLoadedVersion(anyString())).thenReturn(true);
        when(dao.getSyncRefBook(anyString())).thenReturn(new SyncRefBook(1, null, SyncTypeEnum.NOT_VERSIONED, null, null));
        when(syncSourceService.getDiff(argThat(criteria -> criteria != null && criteria.getPageNumber() == 0)))
                .thenReturn(VersionsDiff.dataChangedInstance(pageOf(rec1, rec2, rec3, rec0)));
        when(syncSourceService.getDiff(argThat(criteria -> criteria != null && criteria.getPageNumber() > 0)))
                .thenReturn(VersionsDiff.dataChangedInstance(new PageImpl<>(Collections.emptyList())));
        when(dao.getActualLoadedVersion(anyString())).thenReturn(new LoadedVersion(1, "testCode", "1.1", null, null, null, null));
        String refBookTable = "ref_table";
        when(dao.getVersionMapping(anyString(), anyString())).thenReturn(VersionMapping.builder().id(1).table(refBookTable).build());

        DownloadResult downloadResult = refBookDownloader.download("testCode", "1.0");

        Assert.assertEquals(DownloadResultType.DIFF, downloadResult.getType());
        verify(dao, times(1)).createDiffTempDataTbl("temp_testCode_1_0", refBookTable);
        verify(dao, times(1)).insertDiffAsTempData("temp_testCode_1_0", List.of(rec2.getRow(), rec3.getRow()),List.of(rec1.getRow()), List.of(rec0.getRow()));

    }

    /**
     * Diff возвращает что структура изменилась и скачиваем целиком версию
     */
    @Test
    public void testDownloadWhenDiffByChangedStructure() {
        Page<Map<String, Object>> page1 = pageOf(
                Map.of("ID", BigInteger.valueOf(1), "CODE", "code1", "name", "name1"),
                Map.of("ID", BigInteger.valueOf(2), "CODE", "code2", "name", "name2")

        );
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 0)))
                .thenReturn(page1);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() > 0)))
                .thenReturn(Page.empty());
        when(dao.getLoadedVersion(anyString(), anyString())).thenReturn(null);
        when(dao.existsLoadedVersion(anyString())).thenReturn(true);
        when(dao.getSyncRefBook(anyString())).thenReturn(new SyncRefBook(1, null, SyncTypeEnum.NOT_VERSIONED, null, null));
        when(dao.getActualLoadedVersion(anyString())).thenReturn(new LoadedVersion(1, "testCode", "1.1", null, null, null, null));
        when(syncSourceService.getDiff(any(VersionsDiffCriteria.class))).thenReturn(VersionsDiff.structureChangedInstance());
        String refBookTable = "ref_table";
        when(dao.getVersionMapping(anyString(), anyString())).thenReturn(VersionMapping.builder()
                .table(refBookTable)
                .primaryField("id")
                .id(1)
                .build()
        );

        DownloadResult downloadResult = refBookDownloader.download("test", "1.0");

        Assert.assertEquals(DownloadResultType.VERSION, downloadResult.getType());
        verify(dao, times(1)).createVersionTempDataTbl("temp_test_1_0", refBookTable, "_sync_rec_id",  "id");
        ArgumentCaptor<List<Map<String, Object>>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(dao, times(1)).insertVersionAsTempData(eq("temp_test_1_0"), dataCaptor.capture());
        Assert.assertEquals(1, dataCaptor.getAllValues().size());
        Assert.assertEquals(page1.getContent(), dataCaptor.getAllValues().get(0));

    }

    @SafeVarargs
    private <T> Page<T> pageOf(T... rows) {
        return new PageImpl<>(List.of(rows));
    }
}
