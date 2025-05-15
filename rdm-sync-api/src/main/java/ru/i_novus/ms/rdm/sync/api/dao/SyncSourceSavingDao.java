package ru.i_novus.ms.rdm.sync.api.dao;

public interface SyncSourceSavingDao {

    void save(SyncSource syncSource);

    SyncSource findByCode(String code);

    boolean tableExists(String tableName);
}
