package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;

import java.util.Map;

@Repository
public class SyncSourceDaoImpl implements SyncSourceDao {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void save(SyncSource syncSource) {
        String sql = "INSERT INTO rdm_sync.source\n" +
                "     (code, name, init_values, service)\n" +
                "     VALUES(:code, :name, :init_values, :service)\n" +
                "     ON CONFLICT (code) DO UPDATE\n" +
                "     SET (name, init_values, service) = (:name, :init_values, :service);";
        namedParameterJdbcTemplate.update(sql, Map.of("code", syncSource.getCode(), "name", syncSource.getName(), "init_values", syncSource.getInitValues(), "service",syncSource.getFactoryName()));
    }

    @Override
    public SyncSource findByCode(String code) {
        return namedParameterJdbcTemplate.queryForObject("SELECT name, code, init_values, service FROM rdm_sync.source WHERE code = :code",
                Map.of("code", code),
                (rs, rowNum) -> new SyncSource(rs.getString("name"), rs.getString("code"),  rs.getString("init_values"), rs.getString("service")));
    }

}
