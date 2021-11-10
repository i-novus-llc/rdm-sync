package ru.i_novus.ms.rdm.sync.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

@Configuration
@ConfigurationProperties(prefix = "sync.source.rdm")
public class RdmSourceLoaderService implements SourceLoaderService {

    private RdmSourceProperty property;

    private final SyncSourceDao dao;

    public RdmSourceLoaderService (RdmSourceProperty property, SyncSourceDao dao){
        this.property = property;
        this.dao = dao;
    }

    @Override
    public void load() {

    }
}
