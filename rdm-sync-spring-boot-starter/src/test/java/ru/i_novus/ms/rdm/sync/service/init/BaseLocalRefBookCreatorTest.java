package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseLocalRefBookCreatorTest {

    @Mock
    private SyncSourceDao syncSourceDao;

    @Mock
    private RdmSyncDao dao;

    @Before
    public void setUp() throws Exception {
        when(dao.lockRefBookForUpdate(anyString(), anyBoolean())).thenReturn(true);
    }

    /**
     * Первая загрузка маппинга и создание таблицы
     */
    @Test
    public void testFirstCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();

        List<Object> createTableArgCaptor = new ArrayList<>();
        BaseLocalRefBookCreator creator = getCreator(createTableArgCaptor);
        creator.create(new VersionAndFieldMapping(versionMapping, fieldMappings));
        verify(dao).insertVersionMapping(versionMapping);
        verify(dao).insertFieldMapping(anyInt(), eq(fieldMappings));
        Assert.assertEquals(versionMapping, createTableArgCaptor.get(1));
    }

    /**
     * Маппинг не менялся и таблицы уже созданы
     */
    @Test
    public void testNoCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        when(dao.getVersionMapping(versionMapping.getCode(), "CURRENT")).thenReturn(versionMapping);
        List<Object> createTableArgCaptor = new ArrayList<>();
        BaseLocalRefBookCreator creator = getCreator(createTableArgCaptor);
        creator.create(new VersionAndFieldMapping( versionMapping, fieldMappings));
        verify(dao, never()).insertVersionMapping(any());
        verify(dao, never()).insertFieldMapping(anyInt(), anyList());
        Assert.assertTrue(createTableArgCaptor.isEmpty());
    }

    /**
     * Маппинг изменился, а таблица уже есть
     */
    @Test
    public void testMappingChanged() {
        VersionMapping newVersionMapping = MappingCreator.createVersionMapping();
        newVersionMapping.setMappingVersion(1);
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        VersionMapping oldVersionMapping = MappingCreator.createVersionMapping();
        oldVersionMapping.setMappingId(55);
        when(dao.getVersionMapping(newVersionMapping.getCode(), "CURRENT")).thenReturn(oldVersionMapping);
        List<Object> createTableArgCaptor = new ArrayList<>();
        BaseLocalRefBookCreator creator = getCreator(createTableArgCaptor);
        creator.create(new VersionAndFieldMapping( newVersionMapping, fieldMappings));
        verify(dao).updateCurrentMapping(newVersionMapping);
        verify(dao).insertFieldMapping(oldVersionMapping.getMappingId(), fieldMappings);
        Assert.assertTrue(createTableArgCaptor.isEmpty());
    }

    private BaseLocalRefBookCreator getCreator(List<Object> createTableArgCaptor){
        return  new BaseLocalRefBookCreator("rdm", true, dao, syncSourceDao) {
            @Override
            protected void createTable(String refBookCode, VersionMapping mapping) {
                createTableArgCaptor.add(refBookCode);
                createTableArgCaptor.add(mapping);
            }
        };
    }
}
