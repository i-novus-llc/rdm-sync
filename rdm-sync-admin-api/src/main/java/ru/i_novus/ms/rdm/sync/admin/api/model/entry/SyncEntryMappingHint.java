package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Подсказка по маппингу для версии записи.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncEntryMappingHint {

    private String id;
    private String name;
    private String href;
}
