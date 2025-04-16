package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;
import ru.i_novus.ms.rdm.sync.init.dao.pg.impl.PgTable;

import java.util.List;
import java.util.Map;

public interface VersionedDataDao {

    void addFirstVersionData(String tempTable,
                             PgTable pgTable,
                             Integer versionId);

    void addDiffVersionData(String tempTable,
                            PgTable pgTable,
                            String code,
                            Integer versionId,
                            String syncedVersion);

    void repeatVersion(String tempTable,
                       PgTable pgTable,
                       Integer versionId);

    Page<Map<String, Object>> getData(VersionedLocalDataCriteria localDataCriteria);

    void insertVersions(List<Long> ids,
                         Integer versionId,
                         PgTable pgTable);

}
