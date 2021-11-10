package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class LoadedVersion {

    private Integer id;
    private String code;
    private String version;
    private LocalDateTime publicationDate;
    private LocalDateTime lastSync;

}
