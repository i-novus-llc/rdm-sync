package ru.i_novus.ms.rdm.sync.model.loader;

import org.junit.jupiter.api.Test;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.service.mapping.utils.MappingCreator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class XmlMappingRefBookTest {

    @Test
    public void testGetVersion() {
        XmlMappingRefBook mappingRefBook = new XmlMappingRefBook();

        assertEquals("CURRENT", mappingRefBook.getRefBookVersionIfNullReturnCurrent());

        mappingRefBook.setRefBookVersion("1");
        assertEquals("1", mappingRefBook.getRefBookVersion());
    }

    @Test
    public void testCreateByForRefBookVersion() {
        VersionMapping versionMapping = MappingCreator.createVersionMapping();

        //Ничего не запишет
        versionMapping.setRefBookVersion("CURRENT");
        XmlMappingRefBook mappingRefBook = XmlMappingRefBook.createBy(versionMapping);
        assertNull(mappingRefBook.getRefBookVersion());

        //Запишет версию
        versionMapping.setRefBookVersion("1.0");
        assertEquals("1.0", versionMapping.getRefBookVersion());
    }

}