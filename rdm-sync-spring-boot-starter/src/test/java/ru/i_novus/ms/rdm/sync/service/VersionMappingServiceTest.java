package ru.i_novus.ms.rdm.sync.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionMappingServiceTest {

    @Mock
    private RdmSyncDao rdmSyncDao;

    @InjectMocks
    private VersionMappingServiceImpl versionMappingService;


    /**
     * Проверяем тот случай, когда по версии найден маппинг
     */
    @Test
    void whenVersionMappingNotNull() {
        //Данные для Справочника
        String someRefBookCode = "someRefBookCode";
        String someRefBookVersion = "2.0";

        //Диапазоны, для моков маппинга из БД
        String expectedRange = "2.0-4.0";
        String someRange1 = "*-1.10";
        String someRange2 = "3.0";

        //Создаем список маппингов, для мока данных из БД
        VersionMapping expectedVersionMapping = generateVersionMappingWithSomeRange(expectedRange);
        VersionMapping someVersionMapping1 = generateVersionMappingWithSomeRange(someRange1);
        VersionMapping someVersionMapping2 = generateVersionMappingWithSomeRange(someRange2);

        List<VersionMapping> mockMappingsFromDb = Arrays.asList(
                someVersionMapping1,
                someVersionMapping2,
                expectedVersionMapping
        );

        // Мокаем методы dao
        when(rdmSyncDao.getVersionMappingsByRefBookCode(someRefBookCode)).thenReturn(mockMappingsFromDb);

        VersionMapping actualVersionMapping =
                versionMappingService.getVersionMapping(someRefBookCode, someRefBookVersion);

        assertEquals(expectedVersionMapping, actualVersionMapping);
    }

    /**
     * Проверяем тот случай, когда версия равна null
     */
    @Test
    void whenVersionMappingNull() {
        //Данные для Справочника
        String someRefBookCode = "someRefBookCode";
        String someRefBookVersion = null;

        //Диапазоны, для моков маппинга из БД
        String expectedRange = "2.0-4.0";
        String someRange1 = "*-1.10";
        String someRange2 = "3.0";

        //Создаем список маппингов, для мока данных из БД
        VersionMapping expectedVersionMapping = generateVersionMappingWithSomeRange(expectedRange);
        VersionMapping someVersionMapping1 = generateVersionMappingWithSomeRange(someRange1);
        VersionMapping someVersionMapping2 = generateVersionMappingWithSomeRange(someRange2);

        List<VersionMapping> mockMappingsFromDb = Arrays.asList(
                someVersionMapping1,
                someVersionMapping2,
                expectedVersionMapping
        );

        // Мокаем методы dao
        when(rdmSyncDao.getVersionMappingsByRefBookCode(someRefBookCode)).thenReturn(mockMappingsFromDb);

        VersionMapping actualVersionMapping =
                versionMappingService.getVersionMapping(someRefBookCode, someRefBookVersion);

        assertEquals(expectedVersionMapping, actualVersionMapping);
    }

    /**
     * Проверяем тот случай, когда один из диапазонов равен null
     */
    @Test
    void whenRangeNull() {
        //Данные для Справочника
        String someRefBookCode = "someRefBookCode";
        String someRefBookVersion = "2.0";

        //Диапазоны, для моков маппинга из БД
        String nullRange = null;
        String expectedRange = "2.0-4.0";
        String someRange1 = "3.0";


        //Создаем список маппингов, для мока данных из БД
        VersionMapping someVersionMapping1 = generateVersionMappingWithSomeRange(nullRange);
        VersionMapping someVersionMapping2 = generateVersionMappingWithSomeRange(someRange1);
        VersionMapping expectedVersionMapping = generateVersionMappingWithSomeRange(expectedRange);

        List<VersionMapping> mockMappingsFromDb = Arrays.asList(
                someVersionMapping1,
                someVersionMapping2,
                expectedVersionMapping
        );

        // Мокаем методы dao
        when(rdmSyncDao.getVersionMappingsByRefBookCode(someRefBookCode)).thenReturn(mockMappingsFromDb);

        VersionMapping actualVersionMapping =
                versionMappingService.getVersionMapping(someRefBookCode, someRefBookVersion);

        assertEquals(expectedVersionMapping, actualVersionMapping);
    }

    /**
     * Не найден ни один маппинг вернется null
     */
    @Test
    void whenVersionMappingNotContainsVersion() {
        //Данные для Справочника
        String someRefBookCode = "someRefBookCode";
        String someRefBookVersion = "5.0";

        //Диапазоны, для моков маппинга из БД
        String expectedRange = "2.0-4.0";
        String someRange1 = "*-1.10";
        String someRange2 = "3.0";

        //Создаем список маппингов, для мока данных из БД
        VersionMapping expectedVersionMapping = generateVersionMappingWithSomeRange(expectedRange);
        VersionMapping someVersionMapping1 = generateVersionMappingWithSomeRange(someRange1);
        VersionMapping someVersionMapping2 = generateVersionMappingWithSomeRange(someRange2);

        List<VersionMapping> mockMappingsFromDb = Arrays.asList(
                someVersionMapping1,
                someVersionMapping2,
                expectedVersionMapping
        );

        // Мокаем методы dao
        when(rdmSyncDao.getVersionMappingsByRefBookCode(someRefBookCode)).thenReturn(mockMappingsFromDb);

        VersionMapping actualVersionMapping =
                versionMappingService.getVersionMapping(someRefBookCode, someRefBookVersion);

        assertNull(actualVersionMapping);
    }

    private VersionMapping generateVersionMappingWithSomeRange(String range){
        return new VersionMapping(null, "refBookCode", "refBookName", "", "test_table", "pkSysColumn", "CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED, new Range(range), true, false);
    }

}