package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
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

    @Override
    protected VersionMapping getVersionMapping(String refBookCode, String refBookName, String sourceCode, SyncTypeEnum type, String table, RefBookStructure structure, String sysPkColumn) {
        String sysPkColumnFromUniqueSysField = caseIgnore ? structure.getPrimaries().get(0).toLowerCase() : structure.getPrimaries().get(0);
        return super.getVersionMapping(refBookCode,refBookName,sourceCode,type,table,structure,sysPkColumnFromUniqueSysField);
    }
}
