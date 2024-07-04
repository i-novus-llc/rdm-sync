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
     * Проверяем тот случай, когда версия маппинга не равна null
     */
    @Test
    void whenVersionMappingNotNull() {
        //Данные для Справочника
        String someRefBookCode = "someRefBookCode";
        String someRefBookVersion = "2.0";
        String anotherFailRefBookVersion = "5.0";

        //Диапазоны, для моков маппинга из БД
        Range someRange1 = new Range("*-1.10");
        Range someRange2 = new Range("2.0-4.0");
        Range someRange3 = new Range("3.0");

        //Создаем список маппингов, для мока данных из БД
        List<VersionMapping> mockMappingsFromDb = Arrays.asList(
                new VersionMapping(null, "refBookCode", "refBookName", "", "test_table", "pkSysColumn", "CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED, someRange1, true, false),
                new VersionMapping(null, "refBookCode", "refBookName", "", "test_table", "pkSysColumn", "CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED, someRange2, true, false),
                new VersionMapping(null, "refBookCode", "refBookName", "", "test_table", "pkSysColumn", "CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED, someRange3, true, false)
        );

        // Мокаем методы dao
        when(rdmSyncDao.getVersionMappingsByRefBookCode(someRefBookCode)).thenReturn(mockMappingsFromDb);

        //Запрашиваем получение диапазона версии маппинга
        Range actualRange = versionMappingService.getVersionMapping(someRefBookCode, someRefBookVersion).getRange();

        //Проверяем версии в диапазоне
        assertTrue(actualRange.containsVersion(someRefBookVersion));
        assertFalse(actualRange.containsVersion(anotherFailRefBookVersion));
    }

}