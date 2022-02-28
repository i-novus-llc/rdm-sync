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
}
