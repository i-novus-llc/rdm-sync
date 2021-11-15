package ru.i_novus.ms.fnsi.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

import java.util.Locale;

public class FnsiSourceLoaderService implements SourceLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(FnsiSourceLoaderService.class);

    private final FnsiSourceProperty property;

    private final SyncSourceDao dao;

    @Autowired
    public FnsiSourceLoaderService(FnsiSourceProperty property, SyncSourceDao dao) {
        this.dao = dao;
        this.property = property;
    }

    @Override
    public void load() {
        if (property.getValues() == null) {
            logger.info("No any properties source for load");
        } else {
            property.getValues().forEach(fnsiSourcePropertyValue ->
                    dao.save(new SyncSource(
                            fnsiSourcePropertyValue.getName(),
                            fnsiSourcePropertyValue.getCode(),
                            String.format("{\"userKey\":\"%s\", \"url\":\"%s\"}",
                                    fnsiSourcePropertyValue.getUserKey(), fnsiSourcePropertyValue.getUrl()),
                            FnsiSyncSourceService.class.getSimpleName()
                    )));
        }
    }
}
