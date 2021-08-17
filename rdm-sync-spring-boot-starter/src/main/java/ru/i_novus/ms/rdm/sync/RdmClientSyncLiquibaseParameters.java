package ru.i_novus.ms.rdm.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "liquibase.param")
public class RdmClientSyncLiquibaseParameters {

    private static final String QUARTZ_SCHEMA_NAME = "rdm_sync_qz";
    private static final String QUARTZ_TABLE_PREFIX = "rdm_sync_qrtz_";

    private String quartzSchemaName = QUARTZ_SCHEMA_NAME;

    private String quartzTablePrefix = QUARTZ_TABLE_PREFIX;

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
}
