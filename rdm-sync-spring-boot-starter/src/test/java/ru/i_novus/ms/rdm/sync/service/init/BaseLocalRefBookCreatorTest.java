package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseLocalRefBookCreatorTest {

    @Mock
    private SyncSourceDao syncSourceDao;

    @Mock
    private RdmSyncDao dao;

    @BeforeEach
    public void setUp() throws Exception {
        when(dao.lockRefBookForUpdate(anyString(), anyBoolean())).thenReturn(true);
    }

    /**
     * Первая загрузка маппинга и создание таблицы
     */
    @Test
    void testFirstCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();

        List<Object> createTableArgCaptor = new ArrayList<>();
        BaseLocalRefBookCreator creator = getCreator(createTableArgCaptor);
        creator.create(new SyncMapping(versionMapping, fieldMappings));
        verify(dao).insertVersionMapping(versionMapping);
        verify(dao).insertFieldMapping(anyInt(), eq(fieldMappings));
        Assertions.assertEquals(versionMapping, createTableArgCaptor.get(1));
    }

    /**
     * Маппинг не менялся и таблицы уже созданы
     */
    @Test
    void testNoCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        when(dao.getVersionMapping(versionMapping.getCode(), "CURRENT")).thenReturn(versionMapping);
        List<Object> createTableArgCaptor = new ArrayList<>();
        BaseLocalRefBookCreator creator = getCreator(createTableArgCaptor);
        creator.create(new SyncMapping( versionMapping, fieldMappings));
        verify(dao, never()).insertVersionMapping(any());
        verify(dao, never()).insertFieldMapping(anyInt(), anyList());
        Assertions.assertTrue(createTableArgCaptor.isEmpty());
    }

    /**
     * Маппинг изменился, а таблица уже есть
     */
    @Test
    void testMappingChanged() {
        VersionMapping newVersionMapping = MappingCreator.createVersionMapping();
        newVersionMapping.setMappingVersion(1);
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        VersionMapping oldVersionMapping = MappingCreator.createVersionMapping();
        oldVersionMapping.setMappingId(55);
        when(dao.getVersionMapping(newVersionMapping.getCode(), "CURRENT")).thenReturn(oldVersionMapping);
        List<Object> createTableArgCaptor = new ArrayList<>();
        BaseLocalRefBookCreator creator = getCreator(createTableArgCaptor);
        creator.create(new SyncMapping( newVersionMapping, fieldMappings));
        verify(dao).updateCurrentMapping(newVersionMapping);
        verify(dao).insertFieldMapping(oldVersionMapping.getMappingId(), fieldMappings);
        Assertions.assertTrue(createTableArgCaptor.isEmpty());
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
