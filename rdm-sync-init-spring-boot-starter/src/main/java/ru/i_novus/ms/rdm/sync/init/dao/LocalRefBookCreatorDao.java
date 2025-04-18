package ru.i_novus.ms.rdm.sync.init.dao;

import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LocalRefBookCreatorDao {

    void createTable(String tableName,
                     String refBookCode,
                     VersionMapping mapping,
                     List<FieldMapping> fieldMappings,
                     String refDescription,
                     Map<String, String> fieldDescription);

    Integer addMapping(VersionMapping versionMapping, List<FieldMapping> fieldMappings);

    void updateMapping(Integer oldMappingId, VersionMapping newVersionMapping, List<FieldMapping> fieldMappings);

    List<String> getColumns(String tableName);

    /**
     *
     * @param tableName -
     * @param newFieldMappings - FieldMapping-и новых колонк
     */
    void refreshTable(String tableName,
                      VersionMapping versionMapping,
                      List<FieldMapping> newFieldMappings,
                      String refDescription,
                      Map<String, String> fieldDescription);


    /**
     * Добавляет комментарии к полям и таблице если их нет
     * @param tableName - таблица
     * @param refDescription - описание справочника
     * @param fieldMappings - маппинг полей
     * @param columnDescriptions - описание атрибутов
     */
    void addCommentsIfNotExists(String tableName,
                                VersionMapping versionMapping,
                                List<FieldMapping> fieldMappings,
                                String refDescription,
                                Map<String, String> columnDescriptions);

    boolean tableExists(String tableName);

}
