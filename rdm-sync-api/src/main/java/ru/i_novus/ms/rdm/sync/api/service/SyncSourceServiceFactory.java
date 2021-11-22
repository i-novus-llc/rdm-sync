package ru.i_novus.ms.rdm.sync.api.service;

import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;

public interface SyncSourceServiceFactory {

    SyncSourceService createService(SyncSource source);

    boolean isSatisfied(SyncSource source);

}
