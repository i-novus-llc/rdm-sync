package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceSavingDao;

import java.util.Map;

@Repository
public class SyncSourceSavingDaoImpl implements SyncSourceSavingDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void save(SyncSource syncSource) {
        String sql = "INSERT INTO rdm_sync.source\n" +
                "     (code, name, init_values, service_factory)\n" +
                "     VALUES(:code, :name, :init_values, :serviceFactory)\n" +
                "     ON CONFLICT (code) DO UPDATE\n" +
                "     SET (name, init_values, service_factory) = (:name, :init_values, :serviceFactory);";
        namedParameterJdbcTemplate.update(sql, Map.of("code", syncSource.getCode(), "name", syncSource.getName(), "init_values", syncSource.getInitValues(), "serviceFactory",syncSource.getFactoryName()));
    }

    @Override
    public SyncSource findByCode(String code) {
        return namedParameterJdbcTemplate.queryForObject("SELECT name, code, init_values, service_factory FROM rdm_sync.source WHERE code = :code",
                Map.of("code", code),
                (rs, rowNum) -> new SyncSource(rs.getString("name"), rs.getString("code"),  rs.getString("init_values"), rs.getString("service_factory")));
    }

    @Override
    public boolean tableExists(String tableName) {
        return namedParameterJdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT * FROM information_schema.tables  WHERE table_schema = :schema AND table_name = :table)",
                getSchemaAndTableParams(tableName),
                Boolean.class);
    }

    private Map<String, String> getSchemaAndTableParams(String tableName) {
        String schema;
        String table;
        if (tableName.contains(".")) {
            schema = tableName.split("\\.")[0];
            table = tableName.split("\\.")[1];
        } else {
            schema = "public";
            table = tableName;
        }
        return  Map.of("schema", schema, "table", table);
    }
}
