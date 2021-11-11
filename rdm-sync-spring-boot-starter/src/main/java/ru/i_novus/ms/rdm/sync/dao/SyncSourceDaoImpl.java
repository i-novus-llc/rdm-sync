package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

import java.util.HashMap;
import java.util.Map;

@Repository
public class SyncSourceDaoImpl implements SyncSourceDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void save(SyncSource syncSource) {
        Map<String, Object> mapValues = toMapValues(syncSource);
        String sql = "INSERT INTO rdm_sync.source\n" +
                "      (code, name, init_values)\n" +
                "      VALUES(:code, :name, :init_values)\n" +
                "      ON CONFLICT (code) DO UPDATE\n" +
                "      SET (name, init_values) = (:name, :init_values);";
        namedParameterJdbcTemplate.update(sql, mapValues);
    }

    @Override
    public SyncSource findByCode(String code) {
        SqlParameterSource parameter = new MapSqlParameterSource("code", code);
        return namedParameterJdbcTemplate.query("SELECT * FROM rdm_sync.source WHERE code IN (:code)",
                parameter,
                (rs, rowNum) -> new SyncSource(rs.getString("name"), rs.getString("code"), rs.getString("init_values"))).get(0);
    }

    private Map<String, Object> toMapValues(SyncSource syncSource) {
        Map<String, Object> result = new HashMap<>(3);
        result.put("code", syncSource.getCode());
        result.put("name", syncSource.getName());
        result.put("init_values", syncSource.getInitValues());
        return result;
    }

}
