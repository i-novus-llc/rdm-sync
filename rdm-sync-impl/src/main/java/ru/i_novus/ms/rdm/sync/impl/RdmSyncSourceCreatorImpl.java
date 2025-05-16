package ru.i_novus.ms.rdm.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceCreator;

import java.util.List;

public class RdmSyncSourceCreatorImpl implements SyncSourceCreator {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncSourceCreatorImpl.class);

    private final String url;

    @Autowired
    public RdmSyncSourceCreatorImpl(String url) {
        this.url = url;
    }

    public List<SyncSource> create() {
        return List.of(new SyncSource("RDM", "RDM", url, RdmSyncSourceServiceFactory.class.getSimpleName()));
    }
}
