package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class LoadedVersion {

    private Integer id;
    private String code;
    private String version;
    private LocalDateTime publicationDate;
    private LocalDateTime closeDate;
    private LocalDateTime lastSync;
    private Boolean actual;

}
