package ru.i_novus.ms.rdm.sync.api.mapping;

import org.junit.jupiter.api.Test;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncMappingListTest {

    @Test
    public void testThrowExceptionIfCrossRange(){

        VersionMapping versionMapping1 = createVersionMapping();
        VersionMapping versionMapping2 = createVersionMapping();
        VersionMapping versionMapping3 = createVersionMapping();

        versionMapping1.setRange(new Range("1.10"));
        versionMapping2.setRange(new Range("1.10-3.0"));
        versionMapping3.setRange(new Range("4.0"));

        List<SyncMapping> mappings = List.of(
                new SyncMapping(versionMapping1, null),
                new SyncMapping(versionMapping2, null),
                new SyncMapping(versionMapping3, null)
        );

        assertThrows(RdmSyncException.class, () -> SyncMappingList.validate(mappings));

    }

    private VersionMapping createVersionMapping(){
        return new VersionMapping(null, "EK003", "Справочник", null,
                "rdm.ref_ek003", "_sync_rec_id", "RDM", "id", "deleted_ts",
                null, -1, null, SyncTypeEnum.NOT_VERSIONED, null, true, false);
    }

}