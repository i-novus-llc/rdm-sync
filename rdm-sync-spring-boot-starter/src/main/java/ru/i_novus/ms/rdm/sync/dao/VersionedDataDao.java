package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;

import java.util.List;
import java.util.Map;

public interface VersionedDataDao {

    void addFirstVersionData(String tempTable,
                             String refTable,
                             String pkField,
                             Integer versionId,
                             List<String> fields);

    void addDiffVersionData(String tempTable,
                            String refTable,
                            String pkField,
                            String code,
                            Integer versionId,
                            List<String> fields,
                            String syncedVersion);

    void repeatVersion(String tempTable,
                       String refTable,
                       String pkField,
                       Integer versionId,
                       List<String> fields);

    Page<Map<String, Object>> getData(VersionedLocalDataCriteria localDataCriteria);

    void insertVersions(List<Long> ids,
                         Integer versionId,
                         String refTable);

}
