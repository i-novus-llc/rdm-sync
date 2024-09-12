package ru.i_novus.ms.rdm.sync.model.loader;

import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class XmlMappingRefBook {

    private static final Logger logger = LoggerFactory.getLogger(XmlMappingRefBook.class);

    private String code;

    private String name;

    private List<XmlMappingField> fields;

    private String uniqueSysField;

    private int mappingVersion;

    private String deletedField;

    private String sysTable;

    private String sysPkColumn = "_sync_rec_id";

    private String source;

    private SyncTypeEnum type;

    private String refBookVersion;

    private String range;

    private boolean matchCase = true;

    private boolean refreshableRange;

    @XmlAttribute(name = "code", required = true)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlAttribute(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "field")
    public List<XmlMappingField> getFields() {
        if (fields == null) {
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

    @XmlAttribute
    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    @XmlAttribute(name = "refbook-version")
    public String getRefBookVersion() {
        return refBookVersion;
    }

    public void setRefBookVersion(String refBookVersion) {
        this.refBookVersion = refBookVersion;
    }

    @XmlAttribute(name = "match-case")
    public boolean isMatchCase() {
        return matchCase;
    }

    public void setMatchCase(boolean matchCase) {
        this.matchCase = matchCase;
    }

    @XmlAttribute(name = "refreshable-range")
    public boolean isRefreshableRange() {
        return refreshableRange;
    }

    public void setRefreshableRange(boolean refreshableRange) {
        this.refreshableRange = refreshableRange;
    }

    public SyncMapping convertToSyncMapping() {
        return new SyncMapping(generateVersionMapping(), generateFieldMappings());
    }

    private VersionMapping generateVersionMapping() {
        if (type.equals(SyncTypeEnum.NOT_VERSIONED_WITH_NATURAL_PK)) sysPkColumn = uniqueSysField;

        Range resultRange = null;
        if(refBookVersion != null){
            logger.warn("В маппинге для справочника {} указан deprecated аттрибут refbook-version, " +
                    "используйте вместо него range.Атрибут range не будет использоваться если есть аттрибут refbook-version", code);
            resultRange = new Range(refBookVersion);
        } else if(range != null)  {
            resultRange = new Range(range);
        }


        return new VersionMapping(null, code, name, sysTable, sysPkColumn, source,
                uniqueSysField, deletedField, null, mappingVersion, null, type, resultRange, matchCase, refreshableRange);
    }

    private List<FieldMapping> generateFieldMappings() {
        return getFields().stream().map(XmlMappingField::convertToFieldMapping).collect(Collectors.toList());
    }

    public static XmlMappingRefBook createBy(VersionMapping mapping) {

        XmlMappingRefBook result = new XmlMappingRefBook();

        result.setCode(mapping.getCode());
        result.setName(mapping.getRefBookName());
        result.setMappingVersion(-1);
        result.setUniqueSysField(mapping.getPrimaryField());
        result.setDeletedField(mapping.getDeletedField());
        result.setSysTable(mapping.getTable());
        result.setSysPkColumn(mapping.getSysPkColumn());
        result.setSource(mapping.getSource());
        result.setType(mapping.getType());
        result.setRange(mapping.getRange().getRange());
        return result;
    }
}
