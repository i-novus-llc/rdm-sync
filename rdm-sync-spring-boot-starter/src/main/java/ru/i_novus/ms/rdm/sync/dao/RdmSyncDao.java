package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
public interface RdmSyncDao {

    /**
     * Получить список маппинга справочников НСИ на таблицы клиента.
     *
     * @return Список
     */
    List<VersionMapping> getVersionMappings();

    LoadedVersion getLoadedVersion(String code);

    VersionMapping getVersionMapping(String refbookCode, String version);

    int getLastMappingVersion(String refbookCode);

    /**
     * Получить список маппинга полей справочников НСИ на поля клиента.
     *
     * @param refbookCode код справочника НСИ
     * @return Список
     */
    List<FieldMapping> getFieldMappings(String refbookCode);

    List<Pair<String, String>> getLocalColumnTypes(String schemaTable);

    void insertLoadedVersion(String code, String version, LocalDateTime publishDate);

    void updateLoadedVersion(Integer id, String version, LocalDateTime publishDate);

    /**
     * Получить список значений первичных ключей в таблице клиента.
     *
     * @param schemaTable         таблица справочника на стороне клиента
     * @param primaryFieldMapping маппинг для поля - первичного ключа в таблице клиента
     * @return Список идентификаторов данных справочника клиента
     */
    List<Object> getDataIds(String schemaTable, FieldMapping primaryFieldMapping);

    /**
     * Проверить существование значения первичного ключа в таблице клиента.
     *
     * @param schemaTable  таблица справочника на стороне клиента
     * @param primaryField поле - первичный ключ в таблице клиента
     * @return true, если значение есть в таблице
     */
    boolean isIdExists(String schemaTable, String primaryField, Object primaryValue);

    /**
     * Вставить строку в таблицу клиента.
     *
     * @param schemaTable таблица справочника на стороне клиента
     * @param row         строка с данными
     */
    void insertRow(String schemaTable, Map<String, Object> row, boolean markSynced);

    /**
     * Вставить строки в таблицу
     * @param schemaTable таблица
     * @param rows строки
     * @param markSynced
     */
    void insertRows(String schemaTable, List<Map<String, Object>> rows, boolean markSynced);

    void insertVersionedRows(String schemaTable, List<Map<String, Object>> rows, String version);

    void upsertVersionedRows(String schemaTable, List<Map<String, Object>> rows, String version);

    /**
     * Изменить строку в справочник клиента.
     *
     * @param schemaTable  таблица справочника на стороне клиента
     * @param primaryField поле - первичный ключ в таблице клиента
     * @param row          строка с данными
     */
    void updateRow(String schemaTable, String primaryField, Map<String, Object> row, boolean markSynced);

    void updateRows(String schemaTable, String primaryField, List<Map<String, Object>> row, boolean markSynced);

    /**
     * Пометить запись справочника клиента как (не)удалённую.
     *
     * @param schemaTable    таблица справочника на стороне клиента
     * @param primaryField   поле - первичный ключ в таблице клиента
     * @param isDeletedField поле - признак удаления записи в таблице клиента
     * @param primaryValue   значение первичного ключа записи
     * @param deletedTime    дата удаления, если строка не удаленна то null
     */
    void markDeleted(String schemaTable, String primaryField, String isDeletedField,
                     Object primaryValue, @Nullable LocalDateTime deletedTime, boolean markSynced);

    /**
     * Пометить все записи справочника клиента как (не)удалённые.
     *
     * @param schemaTable    таблица справочника на стороне клиента
     * @param isDeletedField поле - признак удаления записи в таблице клиента
     * @param deletedTime    дата удаления, если строка не удаленна то null
     */
    void markDeleted(String schemaTable, String isDeletedField, LocalDateTime deletedTime, boolean markSynced);

    void log(String status, String refbookCode, String oldVersion, String newVersion, String message, String stack);

    List<Log> getList(LocalDate date, String refbookCode);

    Integer insertVersionMapping(VersionMapping versionMapping);

    void updateCurrentMapping(VersionMapping versionMapping);

    void insertFieldMapping(Integer mappingId, List<FieldMapping> fieldMappings);

    boolean lockRefBookForUpdate(String code, boolean blocking);

    void addInternalLocalRowStateUpdateTrigger(String schema, String table);
    void createOrReplaceLocalRowStateUpdateFunction();
    void addInternalLocalRowStateColumnIfNotExists(String schema, String table);
    void disableInternalLocalRowStateUpdateTrigger(String table);
    void enableInternalLocalRowStateUpdateTrigger(String table);
    boolean existsInternalLocalRowStateUpdateTrigger(String table);

    Page<Map<String, Object>> getData(LocalDataCriteria localDataCriteria);

    Page<Map<String, Object>> getVersionedData(VersionedLocalDataCriteria localDataCriteria);
    <T> boolean setLocalRecordsState(String schemaTable, String pk, List<? extends T> primaryValues,
                                     RdmSyncLocalRowState expectedState, RdmSyncLocalRowState state);

    RdmSyncLocalRowState getLocalRowState(String schemaTable, String pk, Object pv);
    void createSchemaIfNotExists(String schema);
    void createTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String isDeletedFieldName, String sysPkColumn, SyncTypeEnum typeEnum);

    void createVersionedTableIfNotExists(String schema, String table, List<FieldMapping> fieldMappings, String sysPkColumn);

    SyncRefBook getSyncRefBook(String code);
}
