package ru.i_novus.ms.rdm.sync.service.downloader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RefBookDownloaderImplTest {


    private RefBookDownloaderImpl refBookDownloader;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private SyncSourceService syncSourceService;

    @Before
    public void setUp() throws Exception {
        refBookDownloader = new RefBookDownloaderImpl(syncSourceService, dao, 1, 0, 2);
    }

    /**
     * Скачивание версии справочника целиком
     */
    @Test
    public void testAllRefBook() {
        Page<Map<String, Object>> page1 = pageOf(
                Map.of("ID", 1, "CODE", "code1", "name", "name1"),
                Map.of("ID", 2, "CODE", "code2", "name", "name2")

        );
        Page<Map<String, Object>> page2 = pageOf(
                Map.of("ID", 3, "CODE", "code3", "name", "name3"),
                Map.of("ID", 4, "CODE", "code4", "name", "name4")

        );
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 0)))
                .thenReturn(
                        page1);

        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 1)))
                .thenReturn(
                        page2);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() > 1)))
                .thenReturn(Page.empty());

        refBookDownloader.download("code", "1.0");
        verify(dao, times(1)).createTempDataTbl("temp_code_1_0");
        ArgumentCaptor<List<Map<String, Object>>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(dao, times(2)).insertTempData(eq("temp_code_1_0"), dataCaptor.capture());
        Assert.assertEquals(2, dataCaptor.getAllValues().size());
        Assert.assertEquals(page1.getContent(), dataCaptor.getAllValues().get(0));
        Assert.assertEquals(page2.getContent(), dataCaptor.getAllValues().get(1));
    }

    private Page<Map<String, Object>> pageOf(Map<String, Object>... rows) {
        return new PageImpl(List.of(rows));
    }
}
