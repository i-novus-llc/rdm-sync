package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;

public class LockingRdmSyncService extends RdmSyncServiceImpl {

    @Autowired
    private RdmSyncDao dao;

    @Override
    public void update(String refBookCode) {
        if (dao.lockRefBookForUpdate(refBookCode, false))
            super.update(refBookCode);
    }
}
