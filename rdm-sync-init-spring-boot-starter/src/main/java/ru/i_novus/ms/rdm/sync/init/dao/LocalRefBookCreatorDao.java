package ru.i_novus.ms.rdm.sync.init.dao;

import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;

public interface LocalRefBookCreatorDao {

    void createTable(String tableName, String refBookCode, VersionMapping mapping, List<FieldMapping> fieldMappings);

    Integer addMapping(VersionMapping versionMapping, List<FieldMapping> fieldMappings);

    void updateMapping(Integer oldMappingId, VersionMapping newVersionMapping, List<FieldMapping> fieldMappings);

    List<String> getColumns(String tableName);

    /**
     *
     * @param tableName -
     * @param newFieldMappings - FieldMapping-и новых колонк
     */
    void refreshTable(String tableName, List<FieldMapping> newFieldMappings);

    boolean tableExists(String tableName);

}
