package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.init.dao.VersionMappingDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class VersionMappingDaoImpl implements VersionMappingDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper versionMappingRowMapper = (rs, rowNum) -> new VersionMapping(
            rs.getInt(1),
            rs.getString(2),
            rs.getString(3),
            rs.getString(5),
            rs.getString(6),
            rs.getString(7),
            rs.getString(8),
            rs.getString(9),
            toLocalDateTime(rs, 10, LocalDateTime.MIN),
            rs.getInt(11),
            rs.getInt(12),
            SyncTypeEnum.valueOf(rs.getString(13)),
            rs.getString(4) == null ? null : new Range(rs.getString(4)),
            rs.getBoolean(14),
            rs.getBoolean(15)
    );

    public VersionMappingDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<VersionMapping> getVersionMappings() {

        final String sql = "SELECT m.id, code, name, version, \n" +
                "       sys_table, sys_pk_field, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, \n" +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type, match_case, refreshable_range \n" +
                "  FROM rdm_sync.version v \n" +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id \n" +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id \n";

        return namedParameterJdbcTemplate.query(sql, versionMappingRowMapper);
    }

    @Override
    public void deleteVersionMappings(Set<Integer> mappingIds) {
        // Удаляем запись из таблицы rdm_sync.field_mapping по mapping_id
        final String delFieldSql = "DELETE FROM rdm_sync.field_mapping WHERE mapping_id in (:mappingIds);";

        // Удаляем запись из таблицы rdm_sync.version по mapping_id
        final String delVersionSql = "DELETE FROM rdm_sync.version WHERE mapping_id in (:mappingIds);";

        // Удаляем запись из таблицы rdm_sync.mapping по id
        final String delMappingSql = "DELETE FROM rdm_sync.mapping WHERE id in (:mappingIds);";
        namedParameterJdbcTemplate.update(delFieldSql + delVersionSql + delMappingSql, Map.of("mappingIds", mappingIds));
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, int columnIndex, LocalDateTime defaultValue) throws SQLException {

        final Timestamp value = rs.getTimestamp(columnIndex);
        return value != null ? value.toLocalDateTime() : defaultValue;
    }

    @Override
    public VersionMapping getVersionMappingByCodeAndRange(String referenceCode, String range) {
        final String sql = "SELECT m.id, code, name, version, " +
                "       sys_table, sys_pk_field, (SELECT s.code FROM rdm_sync.source s WHERE s.id = r.source_id), unique_sys_field, deleted_field, " +
                "       mapping_last_updated, mapping_version, mapping_id, sync_type, match_case, refreshable_range " +
                "  FROM rdm_sync.version v " +
                " INNER JOIN rdm_sync.mapping m ON m.id = v.mapping_id " +
                " INNER JOIN rdm_sync.refbook r ON r.id = v.ref_id " +
                "WHERE code = :code AND (:range IS NULL OR version = :range);";

        List<VersionMapping> results = namedParameterJdbcTemplate.query(sql, Map.of(
                "code", referenceCode,
                "range", range == null ? "" : range
        ), versionMappingRowMapper);

        return results.isEmpty() ? null : results.get(0);
    }
}
