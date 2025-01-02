package ru.i_novus.ms.rdm.sync.init.init;

import org.junit.jupiter.api.Test;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.init.SyncMappingComparator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncMappingComparatorTest {

    @Test
    void testSyncMappingComparator() {
        SyncMapping sm1 = new SyncMapping(createVersionMapping("1.0"), Collections.emptyList());
        SyncMapping sm2 = new SyncMapping(createVersionMapping("3.2"), Collections.emptyList());
        SyncMapping sm3 = new SyncMapping(createVersionMapping("1.1"), Collections.emptyList());
        SyncMapping sm4 = new SyncMapping(createVersionMapping(null), Collections.emptyList());
        SyncMapping sm5 = new SyncMapping(createVersionMapping("1.0-1.9"), Collections.emptyList());
        SyncMapping sm6 = new SyncMapping(createVersionMapping("2.0-2.9"), Collections.emptyList());

        List<SyncMapping> syncMappings = Arrays.asList(sm1, sm2, sm3, sm4, sm5, sm6);

        // Создаем экземпляр компаратора
        SyncMappingComparator comparator = new SyncMappingComparator();

        // Сортируем список с использованием компаратора
        Collections.sort(syncMappings, comparator);

        // Проверяем, что список отсортирован правильно
        assertEquals(sm4, syncMappings.get(0)); // sm4 должен быть первым, так как range у него null
        assertEquals(sm1, syncMappings.get(1));
        assertEquals(sm3, syncMappings.get(2));
        assertEquals(sm5, syncMappings.get(3));
        assertEquals(sm6, syncMappings.get(4));
        assertEquals(sm2, syncMappings.get(5));
    }

    private VersionMapping createVersionMapping(String range) {
        return new VersionMapping(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                null,
                new Range(range),
                true,
                false
        );
    }
}
