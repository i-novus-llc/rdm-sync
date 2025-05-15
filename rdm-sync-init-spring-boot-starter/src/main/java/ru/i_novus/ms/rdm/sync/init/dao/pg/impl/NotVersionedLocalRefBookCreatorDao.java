package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.PgTable;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class NotVersionedLocalRefBookCreatorDao extends BaseLocalRefBookCreatorDao {

    private static final String RECORD_SYS_COL_INFO = "bigserial PRIMARY KEY";

    NotVersionedLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    @Override
    protected Map<String, String> getAdditionColumns(VersionMapping mapping) {
        return  Map.of(mapping.getDeletedField(), "timestamp without time zone",
                mapping.getSysPkColumn(), RECORD_SYS_COL_INFO);
    }

    @Override
    protected void customizeTable(PgTable pgTable, VersionMapping mapping, List<FieldMapping> fieldMappings) {

        String addUniqueConstraint = "ALTER TABLE {schema}.{table} ADD CONSTRAINT {constraint} UNIQUE({column})";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(StringSubstitutor.replace(
                addUniqueConstraint,
                Map.of("schema", pgTable.getSchema(), "table", pgTable.getTable(), "constraint", pgTable.getUniqueConstraint(), "column", pgTable.getPrimaryField().orElseThrow()), "{", "}"));
    }




}
