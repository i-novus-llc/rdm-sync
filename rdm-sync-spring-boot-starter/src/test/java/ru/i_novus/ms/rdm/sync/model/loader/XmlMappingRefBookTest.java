package ru.i_novus.ms.rdm.sync.model.loader;

import org.junit.Assert;
import org.junit.Test;

public class XmlMappingRefBookTest {

    @Test
    public void testGetVersion() {
        XmlMappingRefBook mappingRefBook = new XmlMappingRefBook();

        Assert.assertEquals("CURRENT", mappingRefBook.getRefBookVersion());

        mappingRefBook.setRefBookVersion("1");
        Assert.assertEquals("1", mappingRefBook.getRefBookVersion());
    }
}