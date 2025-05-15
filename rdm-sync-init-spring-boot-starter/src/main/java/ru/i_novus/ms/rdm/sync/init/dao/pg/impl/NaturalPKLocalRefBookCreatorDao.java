package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.PgTable;

import java.util.List;
import java.util.Map;

@Repository
public class NaturalPKLocalRefBookCreatorDao extends BaseLocalRefBookCreatorDao {

    public NaturalPKLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    @Override
    protected void customizeTable(PgTable pgTable, VersionMapping mapping, List<FieldMapping> fieldMappings) {
        Boolean pkIsExists = namedParameterJdbcTemplate.getJdbcTemplate().queryForObject(
                "SELECT EXISTS (SELECT * FROM pg_constraint WHERE conrelid = ?::regclass and contype = 'p')",
                Boolean.class, pgTable.getName());
        if (!Boolean.TRUE.equals(pkIsExists)) {
            String sql = "ALTER TABLE " + pgTable.getName() + " ADD CONSTRAINT "
                    + pgTable.getPkConstraint() + " PRIMARY KEY (" + pgTable.getSysPkColumn().orElseThrow() + ");";

            namedParameterJdbcTemplate.getJdbcTemplate().execute(sql);
        }
    }


    @Override
    protected Map<String, String> getAdditionColumns(VersionMapping mapping) {
        return  Map.of(mapping.getDeletedField(), "timestamp without time zone");
    }
}
