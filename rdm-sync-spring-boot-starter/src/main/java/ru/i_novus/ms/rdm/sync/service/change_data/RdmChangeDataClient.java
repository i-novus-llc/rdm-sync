package ru.i_novus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils.*;

/**
 * Клиент для экспорта данных в RDM.
 */
public abstract class RdmChangeDataClient {

    private static final Logger logger = LoggerFactory.getLogger(RdmChangeDataClient.class);

    @Autowired
    protected VersionMappingService versionMappingService;

    @Autowired
    protected RdmSyncDao dao;

    @Autowired
    protected RdmChangeDataRequestCallback callback;

    /**
     * Этот метод сам сконвертирует ваши объекты в новый набор {@code Map<String, Object> M}, используя следующие правила:
     * <p>- Если объект не экземпляр класса Map, берём все его поля вплоть до Object-а, переводим их в snake_case, кладём в {@code M}.
     * <p>- Если объект - экземпляр класса Map, берём все её ключи, переводим их в snake_case, кладём в новую {@code M}.
     * <p>- Если локально присутствуют записи в таблице rdm_sync.field_mappings для полей справочника с кодом {@code refBookCode}, применяем описанные там правила к {@code M}.
     * <p/>
     * Правила, по которым строки переводятся в snake_case, могут быть найдены <a href="com.google.common.base.CaseFormat#LOWER_UNDERSCORE">здесь</a>.
     * Пара примеров:
     * <p>1) camelCase -> camel_case
     * <p>2) camelCase123 -> camel_case123
     */
    @Transactional
    public <T extends Serializable> void changeData(String refBookCode,
                                                    List<? extends T> addUpdate, List<? extends T> delete) {

        List<FieldMapping> fieldMappings = dao.getFieldMappings(refBookCode);
        changeData(refBookCode, addUpdate, delete, t -> {
            Map<String, Object> map = tToMap(t, true, null);
            if (!fieldMappings.isEmpty()) {
                reindex(fieldMappings, map);
            }
            return map;
        });
    }

    /**
     * Экспортировать данные в RDM (синхронно или через очередь сообщений, в зависимости от реализации).
     * В зависимости от результатов операции будет вызван соответствующий метод у {@link RdmChangeDataRequestCallback}.
     *
     * @param refBookCode Код справочника
     * @param addUpdate   Записи, которые нужно добавить/изменить в RDM
     * @param delete      Записи, которые нужно удалить из RDM
     * @param map         Функция, преобразовывающая экземпляр класса {@code <T>} в {@code Map<String, Object>}.
     *                    Ключами в мапе должны идти поля в RDM, типы данных должны быть приводимыми.
     * @param <T>         Этот параметр должен реализовывать интерфейс Serializable
     *                    ({@link java.util.HashMap} отлично подойдёт).
     */
    @Transactional
    public <T extends Serializable> void changeData(String refBookCode,
                                                    List<? extends T> addUpdate, List<? extends T> delete,
                                                    Function<? super T, Map<String, Object>> map) {

        VersionMapping mapping = versionMappingService.getVersionMapping(refBookCode, null);
        if (mapping == null || (addUpdate.isEmpty() && delete.isEmpty())) {
            return;
        }

        boolean ensureState = false;
        boolean haveTrigger = dao.existsInternalLocalRowStateUpdateTrigger(mapping.getTable());
        ListIterator<? extends T> it = addUpdate.listIterator(addUpdate.size());
        if (it.hasPrevious() && it.previous() == INTERNAL_TAG) {
            ensureState = true;
            it.remove();
        }

        if (ensureState) {
            List<Object> list = new ArrayList<>(extractSnakeCaseKey(mapping.getPrimaryField(), addUpdate));
            list.addAll(extractSnakeCaseKey(mapping.getPrimaryField(), delete));
            if (haveTrigger){
                dao.disableInternalLocalRowStateUpdateTrigger(mapping.getTable());
            }
            try {
                boolean stateChanged = dao.setLocalRecordsState(mapping.getTable(), mapping.getPrimaryField(),
                        list, RdmSyncLocalRowState.DIRTY, RdmSyncLocalRowState.PENDING);
                if (!stateChanged) {
                    logger.info("State change did not pass. Skipping request on {}.", refBookCode);
                    throw new RdmSyncException();
                }
            } finally {
                if (haveTrigger) {
                    dao.enableInternalLocalRowStateUpdateTrigger(mapping.getTable());
                }
            }
        }

        changeData0(refBookCode, addUpdate, delete, map);
    }

    abstract <T extends Serializable> void changeData0(String refBookCode,
                                                       List<? extends T> addUpdate, List<? extends T> delete,
                                                       Function<? super T, Map<String, Object>> map);

    /**
     * Этот метод сам попытается преобразовать экземпляр класса {@code <T>} в набор {@code Map<String, Object> M}, используя следующие правила:
     * <p>- Если T - экземпляр класса Map, берём все её ключи, переводим их в snake_case, оставляем только те ключи, что содержатся в локальной таблице клиента (смотрим по схеме таблицы) и кладём в {@code M}.
     * <p>- Если T не экземпляр класса Map, берём все поля вплоть до Object-а и применяем ту же самую логику к ним.
     * <p>- Для отсутствующих полей дополняем {@code M} ключами из схемы локальной таблицы.
     * <p/>
     * Правила, по которым строки переводятся в snake_case, могут быть найдены <a href="com.google.common.base.CaseFormat#LOWER_UNDERSCORE">здесь</a>.
     * Пара примеров:
     * <p>1) camelCase -> camel_case
     * <p>2) camelCase123 -> camel_case123
     */
    @Transactional
    public <T extends Serializable> void lazyUpdateData(List<? extends T> addUpdate, String localTable) {

        VersionMapping versionMapping = getVersionMappingByTableOrElseThrow(localTable);
        List<Pair<String, String>> columnTypes = dao.getLocalColumnTypes(versionMapping.getTable());
        lazyUpdateData(addUpdate, localTable, t -> mapForPgInsert(t, columnTypes));
    }

    /**
     * Вставить/Обновить записи в локальной таблице.
     * <p/>
     * Существующие записи и новые записи (проверяется по первичному ключу)
     * из состояния {@link ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState#SYNCED} переходят
     * в состояние {@link ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState#DIRTY}.
     * Со временем они перейдут в состояние {@link ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState#PENDING}.
     * Откуда они могут перейти либо обратно в состояние {@link ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState#SYNCED},
     * либо в состояние {@link ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState#ERROR}.
     *
     * @param addUpdate  Записи, которые нужно вставить/изменить в локальной таблице и, со временем, в RDM.
     * @param localTable Локальная таблица с данными (с явно указанными схемой и названием таблицы)
     * @param toMap      Функция для преобразования экземпляра класса {@code <T>} в {@code Map<String, Object>},
     *                   ключами которой идут соответствующие колонки и типы данных в локальной таблице клиента.
     * @param <T>        Этот параметр должен реализовывать интерфейс Serializable (для единообразия)
     */
    @Transactional
    public <T extends Serializable> void lazyUpdateData(List<? extends T> addUpdate, String localTable,
                                                        Function<? super T, Map<String, Object>> toMap) {

        VersionMapping versionMapping = getVersionMappingByTableOrElseThrow(localTable);
        String primaryField = versionMapping.getPrimaryField();
        String isDeletedField = versionMapping.getDeletedField();
        IdentityHashMap<? super T, Map<String, Object>> identityHashMap = new IdentityHashMap<>();

        for (T t : addUpdate) {
            Map<String, Object> map = toMap.apply(t);
            Object primaryValue = map.get(primaryField);
            if (primaryValue == null)
                throw new RdmSyncException("No primary key found. Primary field: " + primaryField);

            if (!dao.isIdExists(localTable, primaryField, primaryValue))

                dao.insertRow(localTable, map, false);

            else {
                dao.markDeleted(localTable, primaryField, isDeletedField, primaryValue, null, false);
                dao.updateRow(localTable, primaryField, map, false);
            }

            identityHashMap.put(t, map);
        }

        List<FieldMapping> fieldMappings = dao.getFieldMappings(versionMapping.getCode());
        changeData(versionMapping.getCode(), addUpdate, emptyList(), t -> {
            Map<String, Object> map = identityHashMap.get(t);
            reindex(fieldMappings, map);
            return map;
        });
    }

    private VersionMapping getVersionMappingByTableOrElseThrow(String table) {

        return dao.getVersionMappings().stream()
                .filter(vm -> vm.getTable().equals(table))
                .findAny().orElseThrow(() -> new RdmSyncException("No table " + table + " found."));
    }

    /**
     * Формирование запроса на изменение записей справочника в RDM.
     * <p>
     * Возвращаемое значение должно соответствовать ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest.
     *
     * @param refBookCode код справочника
     * @param addUpdate   добавляемые/обновляемые записи
     * @param delete      удаляемые записи
     * @param toMap       конвертер в формат записей RDM
     * @return Запрос на изменение
     */
    public static <T extends Serializable> Map<String, Object> toRdmChangeDataRequest(
            String refBookCode,
            List<? extends T> addUpdate,
            List<? extends T> delete,
            Function<? super T, Map<String, Object>> toMap
    ) {
        final List<Map<String, Object>> toAddUpdateRows = new ArrayList<>();
        final List<Map<String, Object>> toDeleteRows = new ArrayList<>();
        for (T t : addUpdate) toAddUpdateRows.add(toRdmRequestRow(toMap.apply(t)));
        for (T t : delete) toDeleteRows.add(toRdmRequestRow(toMap.apply(t)));

        return toRdmChangeDataRequest(refBookCode, toAddUpdateRows, toDeleteRows);
    }

    /**
     * Формирование записи для изменения в RDM.
     * <p>
     * Возвращаемое значение должно соответствовать ru.i_novus.ms.rdm.api.model.refdata.Row.
     *
     * @param data данные для записи
     * @return Запись
     */
    private static Map<String, Object> toRdmRequestRow(final Map<String, Object> data) {
        return Map.of(
                "data", data
        );
    }

    private static Map<String, Object> toRdmChangeDataRequest(
            final String refBookCode,
            final List<Map<String, Object>> toAddUpdateRows,
            final List<Map<String, Object>> toDeleteRows
    ) {
        return Map.of(
                "refBookCode", refBookCode,
                "rowsToAddOrUpdate", toAddUpdateRows,
                "rowsToDelete", toDeleteRows
        );
    }

}
