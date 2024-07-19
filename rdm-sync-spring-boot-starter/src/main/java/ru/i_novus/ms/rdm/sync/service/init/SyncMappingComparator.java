package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;

import java.util.Comparator;

public class SyncMappingComparator implements Comparator<SyncMapping> {
    @Override
    public int compare(SyncMapping o1, SyncMapping o2) {
        //todo
        String version1 = o1.getVersionMapping().getRange().getRange();
        String version2 = o2.getVersionMapping().getRange().getRange();

        if (version1 == null && version2 == null) {
            return 0; // Оба null, считаются равными
        } else if (version1 == null) {
            return -1; // version1 null, значит o1 < o2
        } else if (version2 == null) {
            return 1; // version2 null, значит o1 > o2
        }

        // Сортировка по убыванию refBookVersion
        return version2.compareTo(version1);
    }
}