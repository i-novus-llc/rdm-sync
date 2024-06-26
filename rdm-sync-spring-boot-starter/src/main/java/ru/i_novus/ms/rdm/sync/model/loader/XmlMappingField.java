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

    private boolean ignoreIfNotExists;

    private String defaultValue;

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

    @XmlAttribute(name = "ignore-if-not-exists")
    public boolean isIgnoreIfNotExists() {
        return ignoreIfNotExists;
    }

    public void setIgnoreIfNotExists(boolean ignoreIfNotExists) {
        this.ignoreIfNotExists = ignoreIfNotExists;
    }

    @XmlAttribute(name = "default-value")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static XmlMappingField createBy(FieldMapping mapping) {

        XmlMappingField result = new XmlMappingField();

        result.setSysDataType(mapping.getSysDataType());
        result.setSysField(mapping.getSysField());
        result.setRdmField(mapping.getRdmField());
        result.setIgnoreIfNotExists(mapping.getIgnoreIfNotExists());
        result.setDefaultValue(mapping.getDefaultValue());

        return result;
    }

    public FieldMapping convertToFieldMapping() {
        return new FieldMapping(sysField, sysDataType, rdmField, ignoreIfNotExists, defaultValue);
    }
}
