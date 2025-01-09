package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Map;

@Repository
public class SimpleVersionedLocalRefBookCreatorDao extends BaseLocalRefBookCreatorDao {

    private static final String RECORD_SYS_COL = "_sync_rec_id";
    private static final String RECORD_SYS_COL_INFO = "bigserial PRIMARY KEY";
    private static final String LOADED_VERSION_REF = "version_id";

    public SimpleVersionedLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    @Override
    protected void customizeTable(PgTable pgTable, VersionMapping mapping, List<FieldMapping> fieldMappings) {
        namedParameterJdbcTemplate.getJdbcTemplate().execute(
                String.format("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES rdm_sync.loaded_version(id)",
                        pgTable.getName(), pgTable.getLoadedVersionFk(), pgTable.getLoadedVersionColumn()
                )
        );
        namedParameterJdbcTemplate.getJdbcTemplate().execute(
                String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s, %s);",
                        pgTable.getName(), pgTable.getUniqueConstraint(), pgTable.getPrimaryField().orElseThrow(), pgTable.getLoadedVersionColumn()));
    }

    @Override
    protected Map<String, String> getAdditionColumns(VersionMapping mapping) {
        return Map.of(LOADED_VERSION_REF, "integer NOT NULL",
                RECORD_SYS_COL, RECORD_SYS_COL_INFO);
    }
}
