package ru.i_novus.ms.rdm.sync.dao;

import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.dao.builder.SqlFilterBuilder;
import ru.i_novus.ms.rdm.sync.dao.criteria.BaseDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.model.filter.FieldFilter;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.sync.util.StringUtils.addDoubleQuotes;

@Slf4j
public class VersionedDataDaoImpl implements VersionedDataDao {

    private static final String RECORD_FROM_DT = "_sync_from_dt";
    private static final String RECORD_TO_DT = "_sync_to_dt";
    private static final String RECORD_HASH = "_sync_hash";

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(.*(--|#|/\\*|\\*\\/|;|\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b).*)",
            Pattern.CASE_INSENSITIVE);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public VersionedDataDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public void addFirstVersionData(String tempTable,
                                    String refTable,
                                    String pkField,
                                    LocalDateTime fromDate,
                                    LocalDateTime toDate,
                                    List<String> fields) {
        String columnsExpression =  fields.stream().map(this::escapeName).collect(joining(","));
        String hashExpression =  fields.stream().map(field -> "coalesce(" + escapeName(field) + "::text, '')").collect(joining("||"));
        Map<String, Object> params = new HashMap<>();
        params.put("fromDt", fromDate);
        params.put("toDt", toDate);
        namedParameterJdbcTemplate.update("INSERT INTO " + escapeName(refTable) + "(" + columnsExpression + ", _sync_from_dt, _sync_to_dt, _sync_hash) " +
                "(SELECT "+ columnsExpression + ", :fromDt, :toDt, md5(" + hashExpression + ") FROM " + escapeName(tempTable) + ")", params);

    }

    @Override
    public void addDiffVersionData(String tempTable,
                                   String refTable,
                                   String pkField,
                                   LocalDateTime fromDate,
                                   LocalDateTime toDate,
                                   List<String> fields) {

        // todo


    }

    @Override
    public Page<Map<String, Object>> getData(LocalDataCriteria localDataCriteria) {
        Map<String, Object> args = new HashMap<>();
        String sql = String.format("%n  FROM %s %n WHERE 1=1 %n",
                escapeName(localDataCriteria.getSchemaTable()));

        if(localDataCriteria.getDateTime() != null) {
            sql = sql + " AND _sync_from_dt <= :dt AND (_sync_to_dt IS NULL OR _sync_to_dt >= :dt)";
            args.put("dt", localDataCriteria.getDateTime());
        }


        Page<Map<String, Object>> data = getData0(sql, args, localDataCriteria, null);
        data.getContent().forEach(row -> {
            row.remove(RECORD_FROM_DT);
            row.remove(RECORD_TO_DT);
            row.remove(RECORD_HASH);
        });

        return data;
    }

    private Page<Map<String, Object>> getData0(String sql, Map<String, Object> args, BaseDataCriteria dataCriteria, String selectSubQuery) {

        SqlFilterBuilder filterBuilder = getFiltersClause(dataCriteria.getFilters());
        if (filterBuilder != null) {
            sql += "\n AND " + filterBuilder.build();
            args.putAll(filterBuilder.getParams());
        }

        Integer count = namedParameterJdbcTemplate.queryForObject("SELECT count(*)" + sql, args, Integer.class);
        if (count == null || count == 0)
            return Page.empty();

        int limit = dataCriteria.getLimit();
        if (limit != 1) {
            sql += String.format("%n ORDER BY %s ", addDoubleQuotes(dataCriteria.getPk()));
        }

        sql += String.format("%n LIMIT %d OFFSET %d", limit, dataCriteria.getOffset());

        sql = "SELECT * " + (selectSubQuery != null ? ", " + selectSubQuery + " " : "")  + sql;

        if (log.isDebugEnabled()) {
            log.debug("getData0 sql:\n{}\n binding args:\n{}\n.", sql, args);
        }

        List<Map<String, Object>> result = namedParameterJdbcTemplate.query(sql,
                args, (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        Object val = rs.getObject(i);
                        String key = rs.getMetaData().getColumnName(i);
                        if (val instanceof Timestamp) {
                            val = ((Timestamp) val).toLocalDateTime();
                        } else if( val instanceof Array) {
                            val = Arrays.asList((Object[]) ((Array)val).getArray());
                        }
                        map.put(key, val);
                    }

                    return map;
                });

        final RestCriteria restCriteria = new DataCriteria();
        restCriteria.setPageNumber(dataCriteria.getOffset() / limit);
        restCriteria.setPageSize(limit);
        restCriteria.setOrders(Sort.by(Sort.Order.asc(dataCriteria.getPk())).get().collect(toList()));

        return new PageImpl<>(result, restCriteria, count);
    }

    /**
     * Получение условия для фильтра по полям.
     */
    private SqlFilterBuilder getFiltersClause(List<FieldFilter> filters) {

        if (CollectionUtils.isEmpty(filters))
            return null;

        SqlFilterBuilder builder = new SqlFilterBuilder();
        filters.forEach(builder::parse);

        return builder;
    }

    private String escapeName(String name) {
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
