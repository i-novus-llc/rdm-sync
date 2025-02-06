package ru.i_novus.ms.rdm.sync.init.description;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

/**
 * Получение описания справочников и его атрибутов
 */
public interface RefBookDescriptionService {

    RefBookDescription getRefBookDescription(SyncMapping mapping);
}
