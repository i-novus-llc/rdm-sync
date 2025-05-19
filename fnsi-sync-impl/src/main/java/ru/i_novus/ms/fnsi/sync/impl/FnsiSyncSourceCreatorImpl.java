package ru.i_novus.ms.fnsi.sync.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceCreator;

import java.util.ArrayList;
import java.util.List;

public class FnsiSyncSourceCreatorImpl implements SyncSourceCreator {

    private static final Logger logger = LoggerFactory.getLogger(FnsiSyncSourceCreatorImpl.class);

    private final FnsiSourceProperty property;

    @Autowired
    public FnsiSyncSourceCreatorImpl(FnsiSourceProperty property) {
        this.property = property;
    }

    public List<SyncSource> create() {
        List<SyncSource> list = new ArrayList<>();
        if (property.getValues() == null) {
            logger.info("No source properties to load");
            return list;
        }

        property.getValues().forEach(propertyValue ->
                list.add(new SyncSource(
                        propertyValue.getName(),
                        propertyValue.getCode(),
                        String.format("{\"userKey\":\"%s\", \"url\":\"%s\"}",
                                propertyValue.getUserKey(), propertyValue.getUrl()),
                        FnsiSyncSourceServiceFactory.class.getSimpleName()
                ))
        );
        return list;
    }
}
