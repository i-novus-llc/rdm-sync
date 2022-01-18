package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.Set;

@Component
public class NotVersionedWithNaturalPrimaryKeyLocalRefBookCreator extends NotVersionedLocalRefBookCreator {
    Logger logger = LoggerFactory.getLogger(NotVersionedWithNaturalPrimaryKeyLocalRefBookCreator.class);

    public NotVersionedWithNaturalPrimaryKeyLocalRefBookCreator(
            @Value("${rdm-sync.auto-create.schema:rdm}") String schema,
            @Value("${rdm-sync.auto-create.ignore-case:true}") Boolean caseIgnore,
            RdmSyncDao dao, SyncSourceDao syncSourceDao,
            Set<SyncSourceServiceFactory> syncSourceServiceFactories) {
        super(schema, caseIgnore, dao, syncSourceDao, syncSourceServiceFactories);
    }

    @Transactional
    @Override
    public void create(String refBookCode, String refBookName, String source, SyncTypeEnum type, String table, String sysPkColumn) {
        if (dao.getVersionMapping(refBookCode, "CURRENT") != null) {
            logger.info(LOG_AUTOCREATE_SKIP, refBookCode);
            return;
        }

        logger.info(LOG_AUTOCREATE_START, refBookCode);

        VersionMapping mapping = createMapping(refBookCode, refBookName, source, type, table, sysPkColumn);
        if (!dao.lockRefBookForUpdate(refBookCode, true))
            return;

        if (mapping != null) {
            createTable(refBookCode, mapping, type);
        }
    }
}
