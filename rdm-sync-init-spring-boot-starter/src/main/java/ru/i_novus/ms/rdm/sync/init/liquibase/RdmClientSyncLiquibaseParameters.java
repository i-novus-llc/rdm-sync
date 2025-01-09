package ru.i_novus.ms.rdm.sync.init.liquibase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdm-sync.liquibase.param")
public class RdmClientSyncLiquibaseParameters {

    private static final String QUARTZ_SCHEMA_NAME = "rdm_sync_qz";
    private static final String QUARTZ_TABLE_PREFIX = "rdm_sync_qrtz_";

    private String quartzSchemaName = QUARTZ_SCHEMA_NAME;

    private String quartzTablePrefix = QUARTZ_TABLE_PREFIX;

    private boolean quartzEnabled;

    public String getQuartzSchemaName() {
        return quartzSchemaName;
    }

    public void setQuartzSchemaName(String quartzSchemaName) {
        this.quartzSchemaName = quartzSchemaName;
    }

    public String getQuartzTablePrefix() {
        return quartzTablePrefix;
    }

    public void setQuartzTablePrefix(String quartzTablePrefix) {
        this.quartzTablePrefix = quartzTablePrefix;
    }

    public boolean isQuartzEnabled() {
        return quartzEnabled;
    }

    public void setQuartzEnabled(boolean quartzEnabled) {
        this.quartzEnabled = quartzEnabled;
    }
}
