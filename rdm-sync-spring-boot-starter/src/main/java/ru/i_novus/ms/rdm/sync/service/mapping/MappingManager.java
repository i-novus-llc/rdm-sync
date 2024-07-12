package ru.i_novus.ms.rdm.sync.service.mapping;

import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;

import java.util.List;

/**
 * MappingManager валидирует список маппингов, для синхронизации
 *  <p> - проверяет маппинги на предмет того что диапазоны маппингов одно справочника не пересекаются. Если есть пересечение, то выбрасывает исключение.
 *  <p> - помечает как удаленный маппинг, для справочника из БД, если нет такого в списке для синхронизации
 *  <p> - добавляет маппинг, для справочника из БД, если появился новый маппинг в списке для синхронизации
 *  <p> - изменяет маппинг при изменении аттрибута mapping-version. Если меняется range маппинга, то помечает как удаленный старый и добавляет новый
 */
public interface MappingManager {

    List<SyncMapping> validateAndGetMappingsToUpdate(List<SyncMapping> mappings);
}
