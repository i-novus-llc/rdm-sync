package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.VersionMappingCreator;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum.INTEGER;

@RunWith(MockitoJUnitRunner.class)
public class NaturalPKLocalRefBookCreatorTest {

    @InjectMocks
    private NaturalPKLocalRefBookCreator creator;

    @Test
    public void testModifyVersionMappingForNaturalPkLocalRefBookCreator() {

        String expectedSysPkColumn = "id";

        RefBookStructure structure = new RefBookStructure(null, List.of("id"), Map.of("id", INTEGER));
        structure.setPrimaries(List.of(expectedSysPkColumn));
        VersionMapping versionMapping = VersionMappingCreator.create();
        VersionMapping modifyVersionMapping = creator.modifyVersionMappingForDifferentCreator(versionMapping);

        String actualSysPkColumn = modifyVersionMapping.getSysPkColumn();

        assertEquals(expectedSysPkColumn, actualSysPkColumn);

    }
}