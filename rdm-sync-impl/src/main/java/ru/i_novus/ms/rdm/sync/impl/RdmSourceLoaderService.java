package ru.i_novus.ms.rdm.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceSavingDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

public class RdmSourceLoaderService implements SourceLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(RdmSourceLoaderService.class);

    private final String url;

    private final SyncSourceSavingDao dao;

    @Autowired
    public RdmSourceLoaderService(String url, SyncSourceSavingDao dao) {
        this.url = url;
        this.dao = dao;
    }

    @Override
    public void load() {

        final SyncSource source = new SyncSource("RDM", "RDM", url, RdmSyncSourceServiceFactory.class.getSimpleName());

        dao.save(source);

        logger.info("Load source with\n name: {} \n code: {} \n init_values: {} \n service: {}",
                source.getName(), source.getCode(), source.getInitValues(), source.getFactoryName());
    }
}
