package ru.i_novus.ms.rdm.sync.model.loader;

import lombok.EqualsAndHashCode;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public class XmlMappingRefBook {

    private String code;

    private List<XmlMappingField> fields;

    private String uniqueSysField;

    private int mappingVersion;

    private String deletedField;

    private String sysTable;

    private String source;

    private String version;

    @XmlAttribute(name = "code", required = true)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlElement(name = "field")
    public List<XmlMappingField> getFields() {
        if(fields == null) {
            fields = new ArrayList<>();
        }
        return fields;
    }

    public void setFields(List<XmlMappingField> fields) {
        this.fields = fields;
    }

    @XmlAttribute(name = "unique-sys-field", required = true)
    public String getUniqueSysField() {
        return uniqueSysField;
    }

    public void setUniqueSysField(String uniqueSysField) {
        this.uniqueSysField = uniqueSysField;
    }

    @XmlAttribute(name = "mapping-version", required = true)
    public int getMappingVersion() {
        return mappingVersion;
    }

    public void setMappingVersion(int mappingVersion) {
        this.mappingVersion = mappingVersion;
    }

    @XmlAttribute(name = "deleted-field", required = true)
    public String getDeletedField() {
        return deletedField;
    }

    public void setDeletedField(String deletedField) {
        this.deletedField = deletedField;
    }

    @XmlAttribute(name = "sys-table", required = true)
    public String getSysTable() {
        return sysTable;
    }

    public void setSysTable(String sysTable) {
        this.sysTable = sysTable;
    }

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public VersionMapping convertToVersionMapping() {
        return new VersionMapping(null, code, version, sysTable, source, uniqueSysField, deletedField, null, mappingVersion, null, SyncTypeEnum.NOT_VERSIONED);
    }

    public static XmlMappingRefBook createBy(VersionMapping mapping) {

        XmlMappingRefBook result = new XmlMappingRefBook();

        result.setCode(mapping.getCode());
        result.setMappingVersion(-1);
        result.setUniqueSysField(mapping.getPrimaryField());
        result.setDeletedField(mapping.getDeletedField());
        result.setSysTable(mapping.getTable());
        result.setSource(mapping.getSource());

        return result;
    }
}
