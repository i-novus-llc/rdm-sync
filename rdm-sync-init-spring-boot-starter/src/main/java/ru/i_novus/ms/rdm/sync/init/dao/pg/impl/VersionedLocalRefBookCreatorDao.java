package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class VersionedLocalRefBookCreatorDao extends BaseLocalRefBookCreatorDao {

    private static final String RECORD_PK_COL = "_sync_rec_id";
    private static final String RECORD_PK_COL_TYPE = "bigserial PRIMARY KEY";
    private static final String RECORD_FROM_DT = "_sync_from_dt";
    private static final String RECORD_TO_DT = "_sync_to_dt";
    private static final String RECORD_HASH = "_sync_hash";

    public VersionedLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    @Override
    protected void customizeTable(PgTable pgTable, VersionMapping mapping, List<FieldMapping> fieldMappings) {
        String query = """
                ALTER TABLE %s
                ADD COLUMN IF NOT EXISTS _sync_from_dt TIMESTAMP NOT NULL,
                ADD COLUMN IF NOT EXISTS _sync_to_dt TIMESTAMP,
                ADD COLUMN IF NOT EXISTS _sync_hash text NOT NULL;
                """;
        namedParameterJdbcTemplate.getJdbcTemplate().update(String.format(query, pgTable.getName()));
    }

    @Override
    protected Map<String, String> getAdditionColumns(VersionMapping mapping) {
        return Map.of(RECORD_PK_COL, RECORD_PK_COL_TYPE,
                RECORD_FROM_DT, "TIMESTAMP NOT NULL",
                RECORD_TO_DT, "TIMESTAMP",
                RECORD_HASH, "TEXT NOT NULL");
    }
}
