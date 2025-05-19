package ru.i_novus.ms.rdm.sync.api.service;

import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;

import java.util.List;

public interface SyncSourceCreator {
    List<SyncSource> create();
}
