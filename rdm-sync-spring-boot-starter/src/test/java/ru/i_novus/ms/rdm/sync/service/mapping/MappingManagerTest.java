package ru.i_novus.ms.rdm.sync.service.mapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * В рамках этого теста проверяем реализацию MappingManager'а,
 * который отбирает маппинги с разными диапазонами(для одного справочника),
 * для дальнейшей загрузки
 */
@ExtendWith(MockitoExtension.class)
class MappingManagerTest {

    @Mock
    private RdmSyncDao dao;

    @InjectMocks
    private MappingManagerImpl manager;

    /**
     * Валидация xml маппинга на предмет того,
     * что диапазоны маппингов одно справочника не пересекаются.
     * Если есть пересечение, то ошибка.
     */
    @Test
    void testIfCrossRangeThenThrowExceptionAndBreak() {
        VersionMapping versionMapping1 = MappingCreator.createVersionMapping();
        VersionMapping versionMapping2 = MappingCreator.createVersionMapping();
        VersionMapping versionMapping3 = MappingCreator.createVersionMapping();

        versionMapping1.setRange(new Range("1.15"));
        versionMapping2.setRange(new Range("1.15-2.0"));
        versionMapping3.setRange(new Range("3.0"));

        List<VersionMapping> mappings = List.of(
                versionMapping1,
                versionMapping2,
                versionMapping3
        );


        // Проверка, что метод выбрасывает исключение при пересечении диапазонов
        assertThrows(RdmException.class, () -> {
            manager.validateAndGetMappingsToUpdate(mappings);
        });
    }

    /**
     * Удаление маппинга,для конкретного справочника из БД,
     * если нет такого в xml маппинге
     */
    @Test
    void testDeleteVersionMapping() {
        VersionMapping vmFromDb1 = MappingCreator.createVersionMapping();
        VersionMapping vmFromDb2 = MappingCreator.createVersionMapping();
        VersionMapping vmFromDb3 = MappingCreator.createVersionMapping();

        vmFromDb1.setRange(new Range("1.0"));
        vmFromDb2.setRange(new Range("2.0"));
        vmFromDb3.setRange(new Range("3.0"));


        List<VersionMapping> mappingsFromDb = List.of(
                vmFromDb1,
                vmFromDb2,
                vmFromDb3
        );

        VersionMapping vmFromSync1 = MappingCreator.createVersionMapping();
        VersionMapping vmFromSync2 = MappingCreator.createVersionMapping();

        vmFromSync1.setRange(new Range("1.0"));
        vmFromSync2.setRange(new Range("3.0"));


        List<VersionMapping> mappingsFromSync = List.of(
                vmFromSync1,
                vmFromSync2
        );


        when(dao.getVersionMappings()).thenReturn(mappingsFromDb);

        manager.validateAndGetMappingsToUpdate(mappingsFromSync);

        // Проверяем, что метод удаления был вызван для vmFromDb2
        verify(dao).markIsDeletedVersionMapping(vmFromDb2);

        // Проверяем, что метод удаления не был вызван для vmFromDb1 и vmFromDb3
        verify(dao, never()).markIsDeletedVersionMapping(vmFromDb1);
        verify(dao, never()).markIsDeletedVersionMapping(vmFromDb3);

    }

    /**
     * Добавление маппинга, для конкретного справочника в БД,
     * если появился новый маппинг в xml
     */
    @Test
    void testAddNewVersionMapping() {
        VersionMapping vmFromDb1 = MappingCreator.createVersionMapping();
        VersionMapping vmFromDb2 = MappingCreator.createVersionMapping();

        vmFromDb1.setRange(new Range("1.0"));
        vmFromDb2.setRange(new Range("2.0"));

        List<VersionMapping> mappingsFromDb = List.of(
                vmFromDb1,
                vmFromDb2
        );

        VersionMapping vmFromSync1 = MappingCreator.createVersionMapping();
        VersionMapping vmFromSync2 = MappingCreator.createVersionMapping();
        VersionMapping vmFromSync3 = MappingCreator.createVersionMapping();

        vmFromSync1.setRange(new Range("1.0"));
        vmFromSync2.setRange(new Range("2.0"));
        vmFromSync3.setRange(new Range("3.0"));

        List<VersionMapping> mappingsFromSync = List.of(
                vmFromSync1,
                vmFromSync2,
                vmFromSync3
        );

        when(dao.getVersionMappings()).thenReturn(mappingsFromDb);

        List<VersionMapping> toUpdate = manager.validateAndGetMappingsToUpdate(mappingsFromSync);

        // Проверяем, что метод вернул правильный список маппингов для обновления
        assertEquals(1, toUpdate.size());
        assertTrue(toUpdate.contains(vmFromSync3));
    }

    /**
     * Изменение конкретного маппинга должно так же происходить
     * только при изменении аттрибута mapping-version.
     * Если меняется range маппинга, то по сути это удаление старого и добавление нового
     */
    @Test
    void testChangeVersionMapping() {
        VersionMapping vmFromDb1 = MappingCreator.createVersionMapping();
        VersionMapping vmFromDb2 = MappingCreator.createVersionMapping();

        vmFromDb1.setRange(new Range("1.0"));
        vmFromDb1.setMappingVersion(1);
        vmFromDb2.setRange(new Range("2.0"));
        vmFromDb2.setMappingVersion(2);

        List<VersionMapping> mappingsFromDb = List.of(
                vmFromDb1,
                vmFromDb2
        );

        VersionMapping vmFromSync1 = MappingCreator.createVersionMapping();
        VersionMapping vmFromSync2 = MappingCreator.createVersionMapping();

        vmFromSync1.setRange(new Range("1.0"));
        vmFromSync1.setMappingVersion(2); // MappingVersion увеличился на 1
        vmFromSync2.setRange(new Range("3.0")); // Изменился range
        vmFromSync2.setMappingVersion(2);

        List<VersionMapping> mappingsFromSync = List.of(
                vmFromSync1,
                vmFromSync2
        );

        when(dao.getVersionMappings()).thenReturn(mappingsFromDb);

        List<VersionMapping> toUpdate = manager.validateAndGetMappingsToUpdate(mappingsFromSync);

        // Проверяем, что метод вернул правильный список маппингов для обновления
        assertEquals(2, toUpdate.size());
        assertTrue(toUpdate.contains(vmFromSync1));
        assertTrue(toUpdate.contains(vmFromSync2));

        // Проверяем, что метод удаления был вызван для vmFromDb2
        verify(dao).markIsDeletedVersionMapping(vmFromDb2);
    }

}