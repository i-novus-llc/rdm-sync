package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;

import java.time.LocalDateTime;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class VersionMapping {

    private Integer id;
    private String code;
    private String refBookName;
    private String version;
    private String table;
    private String source;
    private String primaryField;
    private String deletedField;
    private LocalDateTime mappingLastUpdated;
    private int mappingVersion;
    private Integer mappingId;
    private SyncTypeEnum type;
    private String range;
}
