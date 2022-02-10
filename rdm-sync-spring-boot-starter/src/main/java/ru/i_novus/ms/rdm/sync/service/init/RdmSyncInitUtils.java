package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.util.StringUtils;

public final class RdmSyncInitUtils {

    public static String buildTableNameWithSchema(String refBookCode, String refBookTable, String defaultSchema, boolean caseIgnore) {

        defaultSchema = defaultSchema == null ? "rdm" : defaultSchema;

        String schemaName;
        String tableName;

        if (!StringUtils.isEmpty(refBookTable)) {

            String[] split = refBookTable.split("\\.");
            schemaName = (split.length > 1) ? split[0] : defaultSchema;
            tableName = (split.length > 1) ? split[1] : refBookTable;

        } else {
            schemaName = defaultSchema;
            tableName = refBookCode.replaceAll("[-.]", "_");
            tableName = "ref_" + (caseIgnore ? tableName.toLowerCase() : tableName);
        }

        return String.format("%s.%s", schemaName, tableName);
    }

}
