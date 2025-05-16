package ru.i_novus.ms.rdm.sync.init.dao;

import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;

public interface SyncSourceDao {

    void save(SyncSource syncSource);

    SyncSource findByCode(String code);

    boolean tableExists(String tableName);

}
