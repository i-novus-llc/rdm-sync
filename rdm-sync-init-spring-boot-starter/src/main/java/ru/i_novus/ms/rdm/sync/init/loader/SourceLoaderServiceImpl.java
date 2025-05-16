package ru.i_novus.ms.rdm.sync.init.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.init.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceCreator;

import java.util.List;

@Service
public class SourceLoaderServiceImpl implements SourceLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(SourceLoaderServiceImpl.class);

    private final SyncSourceDao dao;

    private final List<SyncSourceCreator> creators;

    @Autowired
    public SourceLoaderServiceImpl(SyncSourceDao dao, List<SyncSourceCreator> creators) {
        this.dao = dao;
        this.creators = creators;
    }

    @Override
    public void load() {
        creators.forEach(creator -> {
            List<SyncSource> syncSources = creator.create();
            syncSources.forEach(source -> {
                dao.save(source);
                logger.info("Load source with\n name: {} \n code: {} \n init_values: {} \n service: {}",
                        source.getName(), source.getCode(), source.getInitValues(), source.getFactoryName());
            });
        });
    }
}
