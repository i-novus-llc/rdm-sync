package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.*;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class FieldMapping {

    private String sysField;
    private String sysDataType;
    private String rdmField;
    private boolean ignoreIfNotExists;
    private String defaultValue;

    public FieldMapping(String sysField, String sysDataType, String rdmField) {
        this.sysField = sysField;
        this.sysDataType = sysDataType;
        this.rdmField = rdmField;
    }

    public FieldMapping(String sysField, String sysDataType, String rdmField, String defaultValue) {
        this.sysField = sysField;
        this.sysDataType = sysDataType;
        this.rdmField = rdmField;
        this.defaultValue = defaultValue;
    }

    public FieldMapping(String sysField, String sysDataType, String rdmField, boolean ignoreIfNotExists) {
        this.sysField = sysField;
        this.sysDataType = sysDataType;
        this.rdmField = rdmField;
        this.ignoreIfNotExists = ignoreIfNotExists;
    }
}
