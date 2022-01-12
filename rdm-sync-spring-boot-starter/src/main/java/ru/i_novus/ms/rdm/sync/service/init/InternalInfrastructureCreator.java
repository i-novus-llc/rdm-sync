package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.Objects;

@Component
class InternalInfrastructureCreator {

    private static final Logger logger = LoggerFactory.getLogger(InternalInfrastructureCreator.class);

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private AutoCreateRefBookProperty autoCreateRefBookProperty;

    @Transactional
    public void createInternalInfrastructure(String schemaTable, String code,
                                             String isDeletedFieldName,
                                             List<String> autoCreateRefBookCodes) {

        if (!dao.lockRefBookForUpdate(code, true))
            return;

        String[] split = schemaTable.split("\\.");
        String schema = split[0];
        String table = split[1];

        if (autoCreateRefBookCodes.contains(code)) {

            String sysPk = Objects.requireNonNull(autoCreateRefBookProperty.getRefbooks().stream()
                    .filter(property -> code.equals(property.getCode()))
                    .findAny()
                    .orElse(null)).getSysPk();

            dao.createSchemaIfNotExists(schema);
            dao.createTableIfNotExists(schema, table, dao.getFieldMappings(code), isDeletedFieldName, sysPk);
        }

        logger.info("Preparing table {} in schema {}.", table, schema);

        dao.addInternalLocalRowStateColumnIfNotExists(schema, table);
        dao.createOrReplaceLocalRowStateUpdateFunction(); // Мы по сути в цикле перезаписываем каждый раз функцию, это не страшно
        dao.addInternalLocalRowStateUpdateTrigger(schema, table);

        logger.info("Table {} in schema {} successfully prepared.", table, schema);
    }
}
