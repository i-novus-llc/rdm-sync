package ru.i_novus.ms.rdm.sync.dao;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@JdbcTest(properties = {
        "spring.liquibase.change-log=classpath:/rdm-sync-db/baseChangelog.xml",
        "spring.liquibase.parameters.quartz_schema_name=rdm_sync_qz",
        "spring.liquibase.parameters.quartz_table_prefix=rdm_sync_qrtz_"
})
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.OPENTABLE
)
public abstract class BaseDaoTest {
}
