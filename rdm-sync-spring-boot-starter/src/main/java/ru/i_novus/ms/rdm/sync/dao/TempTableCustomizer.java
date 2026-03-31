package ru.i_novus.ms.rdm.sync.dao;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Extension point for customizing temporary table structure during rdm-sync.
 * Implement this interface to add custom columns to temporary tables
 * that don't exist in the target reference table.
 */
public interface TempTableCustomizer {

    /**
     * Called after version temporary table is created to allow structural customization.
     * Version tables are used for full reference book downloads.
     * Use this to add columns, indexes, or constraints specific to your sync process.
     *
     * @param tempTableName Name of the temporary table (e.g., "temp_refbook_1_2_3_version")
     * @param refTableName Name of the reference/target table (e.g., "organization")
     * @param jdbcTemplate JDBC template for executing DDL statements
     */
    void customizeVersionTempTable(String tempTableName, String refTableName, JdbcTemplate jdbcTemplate);

    /**
     * Called after diff temporary table is created to allow structural customization.
     * Diff tables are used for incremental reference book updates (INSERT/UPDATE/DELETE).
     * These tables include a 'diff_type' column to mark operation type.
     * Use this to add columns, indexes, or constraints specific to your sync process.
     *
     * @param tempTableName Name of the temporary table (e.g., "temp_refbook_1_2_3_diff")
     * @param refTableName Name of the reference/target table (e.g., "organization")
     * @param jdbcTemplate JDBC template for executing DDL statements
     */
    void customizeDiffTempTable(String tempTableName, String refTableName, JdbcTemplate jdbcTemplate);
}
