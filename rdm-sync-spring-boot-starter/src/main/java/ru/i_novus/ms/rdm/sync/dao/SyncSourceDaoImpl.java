package ru.i_novus.ms.rdm.sync.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

@Repository
public class SyncSourceDaoImpl implements SyncSourceDao {

    private static final Logger logger = LoggerFactory.getLogger(SyncSourceDaoImpl.class);

    private static final String COLUMN_CODE_NAME = "code";
    private static final String COLUMN_NAME_NAME = "name";
    private static final String COLUMN_INIT_VALUES_NAME = "init_values";

    private static final List<String> COLUMN_NAMES = asList(COLUMN_CODE_NAME, COLUMN_NAME_NAME, COLUMN_INIT_VALUES_NAME);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SyncSourceDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public void save(SyncSource syncSource) {
        List<String> columns = COLUMN_NAMES;
        String fields = collectionToDelimitedString(columns, ", ");
        String params = collectionToDelimitedString(columns, ", ", ":", "");

        Map<String, Object> mapValues = toMapValues(syncSource);

        String sql = "INSERT INTO rdm_sync.source\n" +
                "      (" + fields + ")\n" +
                "VALUES(" + params + ")";

        logger.debug("Insert table row by sql:\n{}\nwith values\n{}", sql, mapValues);

        namedParameterJdbcTemplate.update(sql, mapValues);
    }

    private Map<String, Object> toMapValues(SyncSource syncSource) {
        Map<String, Object> result = new HashMap<>(3);
        result.put(COLUMN_CODE_NAME, syncSource.getCode());
        result.put(COLUMN_NAME_NAME, syncSource.getName());
        result.put(COLUMN_INIT_VALUES_NAME, syncSource.getInitValues());
        return result;
    }


}
