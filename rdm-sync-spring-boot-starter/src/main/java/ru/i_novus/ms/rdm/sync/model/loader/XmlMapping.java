package ru.i_novus.ms.rdm.sync.model.loader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.EqualsAndHashCode;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@XmlRootElement(name = "mapping")
public class XmlMapping {

    public static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(XmlMapping.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmSyncException(e);
        }
    }

    private List<XmlMappingRefBook> refbooks;

    @XmlElement(name = "refbook")
    public List<XmlMappingRefBook> getRefbooks() {
        if(refbooks == null) {
            refbooks = new ArrayList<>();
        }
        return refbooks;
    }

    public void setRefbooks(List<XmlMappingRefBook> refbooks) {
        this.refbooks = refbooks;
    }
}
