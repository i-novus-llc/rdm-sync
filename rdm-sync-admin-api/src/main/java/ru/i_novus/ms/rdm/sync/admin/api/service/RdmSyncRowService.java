package ru.i_novus.ms.rdm.sync.admin.api.service;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.admin.api.model.RdmSyncRow;
import ru.i_novus.ms.rdm.sync.admin.api.model.RdmSyncRowCriteria;

/**
 * Сервис настройки синхронизации справочников.
 */
public interface RdmSyncRowService {

    /**
     * Поиск записей о синхронизации справочников
     *
     * @param criteria критерий поиска
     * @return Список записей
     */
    Page<RdmSyncRow> search(RdmSyncRowCriteria criteria);

    /**
     * Получение записи о синхронизации справочника по коду справочника.
     *
     * @param code код справочника
     * @return Запись
     */
    RdmSyncRow getByCode(String code);
}
