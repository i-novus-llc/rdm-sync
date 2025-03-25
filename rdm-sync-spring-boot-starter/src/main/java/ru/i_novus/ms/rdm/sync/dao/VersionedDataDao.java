package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VersionedDataDao {

    void addFirstVersionData(String tempTable,
                             String refTable,
                             String pkField,
                             LocalDateTime fromDate,
                             LocalDateTime toDate,
                             List<String> fields);

    void addDiffVersionData(String tempTable,
                            String refTable,
                            String pkField,
                            LocalDateTime fromDate,
                            LocalDateTime toDate,
                            List<String> fields);

    Page<Map<String, Object>> getData(LocalDataCriteria localDataCriteria);

}
