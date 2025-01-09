package ru.i_novus.ms.rdm.sync.init.init;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.init.DefaultLocalRefBookCreator;
import ru.i_novus.ms.rdm.sync.init.dao.LocalRefBookCreatorDao;
import ru.i_novus.ms.rdm.sync.init.mapping.utils.MappingCreator;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultLocalRefBookCreatorTest {

    @Mock
    private LocalRefBookCreatorDao dao;

    @Mock
    private VersionMappingService versionMappingService;

    private DefaultLocalRefBookCreator creator;

    @BeforeEach
    void setUp() {
        creator = new DefaultLocalRefBookCreator("rdm", true, dao, versionMappingService);
    }

    /**
     * Первая загрузка маппинга и создание таблицы
     */
    @Test
    void testFirstCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        creator.create(new SyncMapping(versionMapping, fieldMappings));
        verify(dao).addMapping(versionMapping, fieldMappings);
    }

    /**
     * Маппинг не менялся и таблицы уже созданы
     */
    @Test
    void testNoCreate() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();
        List<FieldMapping> fieldMappings = MappingCreator.createFieldMapping();
        when(versionMappingService.getVersionMappingByCodeAndRange(any(), any())).thenReturn(versionMapping);
        when(dao.tableExists(anyString())).thenReturn(true);
        creator.create(new SyncMapping( versionMapping, fieldMappings));
        verify(dao, never()).addMapping(any(VersionMapping.class), anyList());
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
        when(versionMappingService.getVersionMappingByCodeAndRange(eq(oldVersionMapping.getCode()), any())).thenReturn(oldVersionMapping);
        when(dao.tableExists(anyString())).thenReturn(true);
        creator.create(new SyncMapping(newVersionMapping, fieldMappings));
        verify(dao).updateMapping(oldVersionMapping.getMappingId(), newVersionMapping, fieldMappings);
    }
}
