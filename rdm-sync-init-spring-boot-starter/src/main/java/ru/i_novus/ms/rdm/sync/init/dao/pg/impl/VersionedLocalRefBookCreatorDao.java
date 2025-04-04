package ru.i_novus.ms.rdm.sync.init.dao.pg.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Repository
public class VersionedLocalRefBookCreatorDao extends BaseLocalRefBookCreatorDao {

    private static final String RECORD_PK_COL = "_sync_rec_id";
    private static final String RECORD_PK_COL_TYPE = "bigserial PRIMARY KEY";
    private static final String RECORD_FROM_DT = "_sync_from_dt";
    private static final String RECORD_TO_DT = "_sync_to_dt";
    private static final String RECORD_HASH = "_sync_hash";

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(.*(--|#|/\\*|\\*\\/|;|\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b).*)",
            Pattern.CASE_INSENSITIVE);

    public VersionedLocalRefBookCreatorDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    @Override
    protected void customizeTable(PgTable pgTable, VersionMapping mapping, List<FieldMapping> fieldMappings) {

        String name = pgTable.getName().replace("\"", "");

        namedParameterJdbcTemplate.getJdbcTemplate().execute(
                String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s, %s)",
                        pgTable.getName(), pgTable.getUniqueConstraint(), pgTable.getPrimaryField().orElseThrow(), RECORD_HASH));

        //индекс по хэшу и pk
        String indexName = name.substring(name.indexOf(".") + 1) + "_hash_ix";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(
                String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s, %s)",
                        indexName, pgTable.getName(), pgTable.getPrimaryField().orElseThrow(), RECORD_HASH));

        String query = """
                CREATE TABLE %s (
                id BIGSERIAL PRIMARY KEY,
                record_id BIGINT NOT NULL,
                _sync_from_dt TIMESTAMP NOT NULL,
                _sync_to_dt TIMESTAMP,
                CONSTRAINT record_id_fk FOREIGN KEY (record_id) REFERENCES %s (_sync_rec_id) ON DELETE NO ACTION ON UPDATE NO ACTION
              );
              """;

        String intervalsName = name + "_intervals";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(String.format(query, escapeName(intervalsName), pgTable.getName()));

        //индекс по датам и внешнему ключу
        indexName = intervalsName.substring(intervalsName.indexOf(".") + 1) + "_record_id_ix";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(
                String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)",
                        indexName, intervalsName, "record_id"));

        indexName = intervalsName.substring(intervalsName.indexOf(".") + 1) + "_dates_ix";
        namedParameterJdbcTemplate.getJdbcTemplate().execute(
                String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s, %s)",
                        indexName, intervalsName, RECORD_FROM_DT, RECORD_TO_DT));
    }

    @Override
    protected Map<String, String> getAdditionColumns(VersionMapping mapping) {
        return Map.of(RECORD_PK_COL, RECORD_PK_COL_TYPE,
                RECORD_HASH, "TEXT NOT NULL");
    }

    public static String escapeName(String name) {
        if ( SQL_INJECTION_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(name + "illegal value");
        }
        if (name.contains(".")) {
            String firstPart = escapeName(name.split("\\.")[0]);
            String secondPart = escapeName(name.split("\\.")[1]);
            return firstPart + "." + secondPart;
        }
        return "\"" + name + "\"";

    }
}
