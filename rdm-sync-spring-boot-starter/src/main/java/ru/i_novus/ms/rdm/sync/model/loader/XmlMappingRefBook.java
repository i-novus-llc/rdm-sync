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

    private String name;

    private List<XmlMappingField> fields;

    private String uniqueSysField;

    private int mappingVersion;

    private String deletedField;

    private String sysTable;

    private String sysPkColumn;

    private String source;

    private SyncTypeEnum type;

    private String version;

    @XmlAttribute(name = "code", required = true)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlAttribute(name = "name", required = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @XmlAttribute(name = "sys-pk-field")
    public String getSysPkColumn() {
        return sysPkColumn;
    }

    public void setSysPkColumn(String sysPkColumn) {
        this.sysPkColumn = sysPkColumn;
    }


    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @XmlAttribute
    public SyncTypeEnum getType() {
        return type;
    }

    public void setType(SyncTypeEnum type) {
        this.type = type;
    }

    public VersionMapping convertToVersionMapping() {
        return new VersionMapping(null, code, name, version, sysTable, source, uniqueSysField, deletedField, null, mappingVersion, null, type);
    }

    public static XmlMappingRefBook createBy(VersionMapping mapping) {

        XmlMappingRefBook result = new XmlMappingRefBook();

        result.setCode(mapping.getCode());
        result.setName(mapping.getRefBookName());
        result.setMappingVersion(-1);
        result.setUniqueSysField(mapping.getPrimaryField());
        result.setDeletedField(mapping.getDeletedField());
        result.setSysTable(mapping.getTable());
        result.setSource(mapping.getSource());
        result.setType(mapping.getType());

        return result;
    }
}
