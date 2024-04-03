package ru.i_novus.ms.rdm.sync.service.updater;

import org.mockito.Mock;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

public class AbstractRefBookUpdaterTest {

    @Mock
    private RdmLoggingService loggingService;

    protected RefBookVersion createRefbook() {
        RefBookStructure structure = new RefBookStructure();
        structure.setPrimaries(Arrays.asList("id"));
        structure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.STRING));

        RefBookVersion refBook = new RefBookVersion();
        refBook.setStructure(structure);
        refBook.setVersion("-1");
        refBook.setCode("code");
        refBook.setFrom(LocalDateTime.of(2017, 1, 14, 10, 34));
        refBook.setVersionId(1);

        return refBook;
    }

    protected VersionMapping createVersionMapping(SyncTypeEnum syncType) {
        return new VersionMapping(1, "code", "-1", "1", "table", "","source", "id", "",LocalDateTime.of(2017, 1, 14, 10, 34), -1, 1, syncType, null, true);
    }

    protected FieldMapping createFieldMapping() {
        return new FieldMapping("id", "dataType", "id");
    }

    protected LoadedVersion createLoadedVersion() {
        return new LoadedVersion(1, "code", "-1", LocalDateTime.of(2017, 1, 14, 10, 34), null, LocalDateTime.of(2017, 1, 14, 10, 34), true);
    }


}
