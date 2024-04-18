package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.jupiter.api.Test;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncMappingComparatorTest {

    @Test
    void testSyncMappingComparator() {
        SyncMapping sm1 = new SyncMapping(createVersionMapping("1.0"), Collections.emptyList());
        SyncMapping sm2 = new SyncMapping(createVersionMapping("3.2"), Collections.emptyList());
        SyncMapping sm3 = new SyncMapping(createVersionMapping("1.1"), Collections.emptyList());
        SyncMapping sm4 = new SyncMapping(createVersionMapping(null), Collections.emptyList());


        List<SyncMapping> syncMappings = Arrays.asList(sm1, sm2, sm3, sm4);

        // Создаем экземпляр компаратора
        SyncMappingComparator comparator = new SyncMappingComparator();

        // Сортируем список с использованием компаратора
        Collections.sort(syncMappings, comparator);

        // Проверяем, что список отсортирован правильно
        assertEquals(sm4, syncMappings.get(0)); // sm4 должен быть первым, так как refBookVersion у него null
        assertEquals(sm2, syncMappings.get(1));
        assertEquals(sm3, syncMappings.get(2));
        assertEquals(sm1, syncMappings.get(3));
    }

    private VersionMapping createVersionMapping(String refBookVersion) {
        return new VersionMapping(
                null,
                null,
                null,
                refBookVersion,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                null,
                null,
                true,
                false
        );
    }


}