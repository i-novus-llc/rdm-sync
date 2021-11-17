package ru.i_novus.ms.rdm.sync;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.init.NotVersionedLocalRefBookCreator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotVersionedLocalRefBookCreatorTest {

    @InjectMocks
    private NotVersionedLocalRefBookCreator creator;

    @Mock
    private RdmSyncDao rdmSyncDao;

    @Mock
    private RdmSyncService rdmSyncService;

    @Test
    public void testCreate() {
        Integer mappingId = 1;
        String code = "test.code";
        List<FieldMapping> expectedFieldMappingList = List.of(new FieldMapping("id", "integer", "id"), new FieldMapping("name", "varchar", "name"));
        RefBook refBook = new RefBook();
        refBook.setCode(code);
        refBook.setStructure(
                new RefBookStructure(null,
                        Collections.singletonList("id"),
                        Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING)
                )
        );

        when(rdmSyncDao.lockRefBookForUpdate(eq(code), eq(true))).thenReturn(true);
        when(rdmSyncService.getLastPublishedVersion(code)).thenReturn(refBook);
        when(rdmSyncDao.insertVersionMapping(any())).thenReturn(mappingId);
        when(rdmSyncDao.getFieldMappings(eq(code))).thenReturn(expectedFieldMappingList);

        creator.create(code, null);

        ArgumentCaptor<VersionMapping> mappingCaptor = ArgumentCaptor.forClass(VersionMapping.class);
        verify(rdmSyncDao, times(1)).insertVersionMapping(mappingCaptor.capture());
        Assert.assertEquals(code, mappingCaptor.getValue().getCode());
        Assert.assertEquals(-1, mappingCaptor.getValue().getMappingVersion());
        Assert.assertEquals("is_deleted", mappingCaptor.getValue().getDeletedField());
        Assert.assertEquals("rdm.ref_test_code", mappingCaptor.getValue().getTable());
        Assert.assertEquals("id", mappingCaptor.getValue().getPrimaryField());

        verify(rdmSyncDao, times(1)).insertFieldMapping(eq(1), argThat(ignoreOrderEqList(expectedFieldMappingList)));

        verify(rdmSyncDao, times(1)).createSchemaIfNotExists("rdm");

        verify(rdmSyncDao, times(1))
                .createTableIfNotExists(
                        eq("rdm"),
                        eq("ref_test_code"),
                        argThat(ignoreOrderEqList(expectedFieldMappingList)),
                        eq("is_deleted"));


    }

    /**
     * маппинг уже есть, поэтому игнорируем
     */
    @Test
    public void testIgnoreCreateWhenRefBookWasLoaded() {
        String code = "testCode";
        when(rdmSyncDao.getVersionMapping(code, "CURRENT")).thenReturn(mock(VersionMapping.class));
        creator.create(code, "someSource");
        verify(rdmSyncDao, never()).insertVersionMapping(any());
        verify(rdmSyncDao, never()).insertVersionMapping(any());

    }



    private <T> ArgumentMatcher<List<T>> ignoreOrderEqList(List<T> expectedList) {
        return list -> list.size()==expectedList.size() && list.containsAll(expectedList) && expectedList.containsAll(list);
    }


}
