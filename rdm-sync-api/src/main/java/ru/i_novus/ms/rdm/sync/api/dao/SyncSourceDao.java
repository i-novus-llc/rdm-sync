package ru.i_novus.ms.rdm.sync.api.dao;

public interface SyncSourceDao {

    void save(SyncSource syncSource);

    SyncSource findByCode(String code);

    boolean tableExists(String tableName);

}
