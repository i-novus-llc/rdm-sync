package ru.i_novus.ms.rdm.sync.util;

import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.Comparator;

public class VersionMappingComparator implements Comparator<VersionMapping> {

    @Override
    public int compare(VersionMapping v1, VersionMapping v2) {
        Range range1 = v1.getRange();
        Range range2 = v2.getRange();

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
