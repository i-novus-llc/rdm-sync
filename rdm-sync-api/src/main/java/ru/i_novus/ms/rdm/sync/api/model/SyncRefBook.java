package ru.i_novus.ms.rdm.sync.api.model;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class SyncRefBook {
    private Integer id;
    private String code;
    private SyncTypeEnum type;
    private String name;
    private Set<String> range;
}
