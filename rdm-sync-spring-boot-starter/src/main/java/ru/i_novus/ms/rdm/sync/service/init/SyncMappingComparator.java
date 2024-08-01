package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;

import java.util.Comparator;

public class SyncMappingComparator implements Comparator<SyncMapping> {
    @Override
    public int compare(SyncMapping o1, SyncMapping o2) {
        Range range1 = o1.getVersionMapping().getRange();
        Range range2 = o2.getVersionMapping().getRange();

        if (range1 == null && range2 == null) {
            return 0; // Оба null, считаются равными
        } else if (range1 == null) {
            return 1; // range1 null, значит o1 > o2
        } else if (range2 == null) {
            return -1; // range2 null, значит o1 < o2
        }

        // Используем метод compareTo из Range для сравнения
        return range1.compareTo(range2);
    }
}