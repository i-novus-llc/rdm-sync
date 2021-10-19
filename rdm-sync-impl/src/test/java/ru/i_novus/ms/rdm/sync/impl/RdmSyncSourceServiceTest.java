package ru.i_novus.ms.rdm.sync.impl;


import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RdmSyncSourceServiceTest {

    @InjectMocks
    private RdmSyncSourceService rdmSynсSourceService;

    @Mock
    private RefBookService refBookService;

    @Test
    public void testGetRefBook() {
        String testCode = "testCode";
        ru.i_novus.ms.rdm.api.model.refbook.RefBook rdmRefBook = new ru.i_novus.ms.rdm.api.model.refbook.RefBook();
        Structure structure = new Structure(
                Arrays.asList(
                        Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, ""),
                        Structure.Attribute.build("name", "Наименование", FieldType.STRING, ""),
                        Structure.Attribute.build("ref", "Ссылка", FieldType.REFERENCE, "")
                ),
                Collections.singletonList(new Structure.Reference(null, "testRefCode", ""))
        );
        rdmRefBook.setStructure(structure);
        rdmRefBook.setCode(testCode);
        when(refBookService.search(any(RefBookCriteria.class))).thenReturn(new RestPage<>(Arrays.asList(rdmRefBook)));
        RefBook refBook = rdmSynсSourceService.getRefBook(testCode);
        Assert.assertEquals(rdmRefBook.getCode(), refBook.getCode());
    }
}
