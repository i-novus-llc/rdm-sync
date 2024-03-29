package ru.i_novus.ms.rdm.sync.model.loader;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;

import javax.xml.bind.annotation.XmlAttribute;

@EqualsAndHashCode
@ToString
public class XmlMappingField {

    private String rdmField;

    private String sysDataType;

    private String sysField;

    @XmlAttribute(name = "rdm-field", required = true)
    public String getRdmField() {
        return rdmField;
    }

    public void setRdmField(String rdmField) {
        this.rdmField = rdmField;
    }

    @XmlAttribute(name = "sys-data-type", required = true)
    public String getSysDataType() {
        return sysDataType;
    }

    public void setSysDataType(String sysDataType) {
        this.sysDataType = sysDataType;
    }

    @XmlAttribute(name = "sys-field", required = true)
    public String getSysField() {
        return sysField;
    }

    public void setSysField(String sysField) {
        this.sysField = sysField;
    }

    public static XmlMappingField createBy(FieldMapping mapping) {

        XmlMappingField result = new XmlMappingField();

        result.setSysDataType(mapping.getSysDataType());
        result.setSysField(mapping.getSysField());
        result.setRdmField(mapping.getRdmField());

        return result;
    }

    public FieldMapping convertToFieldMapping() {
        return new FieldMapping(sysField, sysDataType, rdmField);
    }
}
