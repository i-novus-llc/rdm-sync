package ru.i_novus.ms.rdm.sync.api.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class SyncRefBook {
    private Integer id;
    private String code;
    private SyncTypeEnum type;
    private String name;
    private String range;
}
