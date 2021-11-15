package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class FieldMapping {

    private String sysField;
    private String sysDataType;
    private String rdmField;
}
