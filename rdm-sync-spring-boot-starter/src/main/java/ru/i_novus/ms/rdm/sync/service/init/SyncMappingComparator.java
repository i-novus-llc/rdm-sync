package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;

import java.util.Comparator;

public class SyncMappingComparator implements Comparator<SyncMapping> {
    @Override
    public int compare(SyncMapping o1, SyncMapping o2) {
        if ((o1.getVersionMapping().getRefBookVersion() == null || "CURRENT".equals(o1.getVersionMapping().getRefBookVersion())) && o2.getVersionMapping().getRefBookVersion() != null) {
            return -1; // o1 < o2
        } else if (o1.getVersionMapping().getRefBookVersion() != null && o2.getVersionMapping().getRefBookVersion() == null) {
            return 1; // o1 > o2
        }

        // Сортировка по убыванию refBookVersion
        return o2.getVersionMapping().getRefBookVersion().compareTo(o1.getVersionMapping().getRefBookVersion());
    }
}