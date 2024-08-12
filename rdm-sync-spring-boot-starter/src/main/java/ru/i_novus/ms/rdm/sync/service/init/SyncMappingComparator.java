package ru.i_novus.ms.rdm.sync.service.init;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.util.VersionMappingComparator;

import java.util.Comparator;

public class SyncMappingComparator implements Comparator<SyncMapping> {
    @Override
    public int compare(SyncMapping o1, SyncMapping o2) {
        return new VersionMappingComparator().compare(o1.getVersionMapping(), o2.getVersionMapping());
    }
}