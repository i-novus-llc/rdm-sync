package ru.i_novus.ms.rdm.sync.api.mapping;

import org.junit.jupiter.api.Test;
import ru.i_novus.ms.rdm.sync.api.exception.MappingOverlapsException;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MappingOverlapsValidatorTest {

    @Test
    void testThrowExceptionIfCrossRange(){

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

        MappingOverlapsException mappingOverlapsException = assertThrows(MappingOverlapsException.class, () -> MappingOverlapsValidator.validate(mappings));
        assertEquals("refBookCode", mappingOverlapsException.getRefBookCode());
        assertEquals("1.10", mappingOverlapsException.getOverlappingRange1());
        assertEquals("1.10-3.0", mappingOverlapsException.getOverlappingRange2());

    }

    @Test
    void testThrowExceptionIfCrossRangeWithAsterisk(){

        VersionMapping versionMapping1 = createVersionMapping();
        VersionMapping versionMapping2 = createVersionMapping();
        VersionMapping versionMapping3 = createVersionMapping();

        versionMapping1.setRange(new Range("1.11"));
        versionMapping2.setRange(new Range("1.10-*"));

        List<SyncMapping> mappings = List.of(
                new SyncMapping(versionMapping1, null),
                new SyncMapping(versionMapping2, null),
                new SyncMapping(versionMapping3, null)
        );

        MappingOverlapsException mappingOverlapsException = assertThrows(MappingOverlapsException.class, () -> MappingOverlapsValidator.validate(mappings));
        assertEquals("refBookCode", mappingOverlapsException.getRefBookCode());
        assertEquals("1.11", mappingOverlapsException.getOverlappingRange1());
        assertEquals("1.10-*", mappingOverlapsException.getOverlappingRange2());

    }

    @Test
    void testSuccessValidation() {
        VersionMapping versionMapping1 = createVersionMapping();
        VersionMapping versionMapping2 = createVersionMapping();

        versionMapping1.setRange(new Range("1.10"));
        versionMapping2.setRange(new Range("2.10-3.0"));

        List<SyncMapping> mappings = List.of(
                new SyncMapping(versionMapping1, null),
                new SyncMapping(versionMapping2, null)
        );
        assertAll(() -> MappingOverlapsValidator.validate(mappings));
    }

    private VersionMapping createVersionMapping(){
        return new VersionMapping(null, "refBookCode", "refBookName", "test_table", "pkSysColumn", "CODE-1", "id", "deleted_ts", null, -1, null, SyncTypeEnum.NOT_VERSIONED, null, true, false);
    }

}