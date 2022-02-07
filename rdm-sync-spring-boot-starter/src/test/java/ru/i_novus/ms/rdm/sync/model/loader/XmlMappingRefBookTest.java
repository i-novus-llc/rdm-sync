package ru.i_novus.ms.rdm.sync.model.loader;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.service.init.XmlMappingLoaderServiceTest;

public class XmlMappingRefBookTest {

    @Test
    public void testGetVersion() {
        XmlMappingRefBook mappingRefBook = new XmlMappingRefBook();

        Assert.assertEquals("CURRENT", mappingRefBook.getRefBookVersion());

        mappingRefBook.setRefBookVersion("1");
        Assert.assertEquals("1", mappingRefBook.getRefBookVersion());
    }

    @Test
    public void testCreateByForRefBookVersion() {
        VersionMapping versionMapping = XmlMappingLoaderServiceTest.generateVersionMapping();

        //Ничего не запишет
        versionMapping.setRefBookVersion("CURRENT");
        XmlMappingRefBook mappingRefBook = XmlMappingRefBook.createBy(versionMapping);
        Assert.assertNull(mappingRefBook.getRefBookVersion());

        //Запишет версию
        versionMapping.setRefBookVersion("1.0");
        Assert.assertEquals("1.0", versionMapping.getRefBookVersion());
    }

}