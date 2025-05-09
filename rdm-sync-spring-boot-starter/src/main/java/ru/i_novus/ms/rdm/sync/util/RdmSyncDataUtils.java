package ru.i_novus.ms.rdm.sync.util;

import com.google.common.base.CaseFormat;
import org.springframework.data.util.Pair;
import org.springframework.util.ReflectionUtils;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

public final class RdmSyncDataUtils {

    public static final HashMap<String, Object> INTERNAL_TAG = new HashMap<>();

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(.*(--|#|/\\*|\\*\\/|;|\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b).*)",
            Pattern.CASE_INSENSITIVE);

    private RdmSyncDataUtils() {
        throw new UnsupportedOperationException();
    }

    public static void reindex(List<FieldMapping> fieldMappings, Map<String, Object> map) {

        Set<String> sysProcessed = new HashSet<>();
        for (FieldMapping fieldMapping : fieldMappings) {

            String sys = fieldMapping.getSysField();
            String rdm = fieldMapping.getRdmField();
            if (sysProcessed.contains(rdm))
                continue;

            if (!sys.equals(rdm)) {
                Object v1 = map.get(sys);
                Object v2 = map.get(rdm);
                map.put(rdm, v1);
                if (v2 != null) {
                    map.put(sys, v2);
                }
            }

            sysProcessed.add(sys);
        }
    }

    @SuppressWarnings({"squid:S3776", "squid:S134"})
    public static <T extends Serializable> List<Object> extractSnakeCaseKey(String snakeCaseKey, List<? extends T> ts) {

        if (ts.isEmpty())
            return emptyList();

        List<Object> list = new ArrayList<>();
        for (T t : ts) {
            var v = new Object() {
                Object val = null;
            };

            if (t instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) t;
                if (m.containsKey(snakeCaseKey))
                    v.val = m.get(snakeCaseKey);
                else {
                    for (Map.Entry<String, Object> e : m.entrySet()) {
                        if (camelCaseToSnakeCase(e.getKey()).equals(snakeCaseKey)) {
                            v.val = e.getValue();
                            break;
                        }
                    }
                }
            } else {
                ReflectionUtils.doWithFields(t.getClass(), field -> {
                    if (camelCaseToSnakeCase(field.getName()).equals(snakeCaseKey)) {
                        field.setAccessible(true);
                        v.val = field.get(t);
                    }
                });
            }

            if (v.val != null)
                list.add(v.val);
        }
        return list;
    }

    private static String camelCaseToSnakeCase(String s) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s);
    }


    public static <T extends Serializable> Map<String, Object> mapForPgInsert(T t, List<Pair<String, String>> columnTypes) {

        Set<String> columnsSnakeCase = columnTypes.stream().map(Pair::getFirst).collect(toSet());
        return tToMap(t, true, columnsSnakeCase);
    }

    @SuppressWarnings("squid:S3776")
    public static <T extends Serializable> Map<String, Object> tToMap(T t, final boolean toSnakeCase, Set<String> schema) {

        Map<String, Object> map = new HashMap<>();
        if (!(t instanceof Map)) {
            ReflectionUtils.doWithFields(t.getClass(), field -> {
                field.setAccessible(true);
                String snakeCase = camelCaseToSnakeCase(field.getName());
                if (shouldConsiderThisField(snakeCase, schema))
                    map.put(toSnakeCase ? snakeCase : field.getName(), field.get(t));
            });

        } else {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) t).entrySet()) {
                String snakeCase = camelCaseToSnakeCase(e.getKey());
                if (shouldConsiderThisField(snakeCase, schema))
                    map.put(toSnakeCase ? snakeCase : e.getKey(), e.getValue());
            }
        }

        if (schema != null) {
            for (String s : schema) {
                map.putIfAbsent(s, null);
            }
        }
        return map;
    }

    private static boolean shouldConsiderThisField(String fieldName, Set<String> schema) {

        return schema == null || schema.contains(fieldName);
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
