package ru.i_novus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;
import ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils;

import java.io.Serializable;
import java.util.List;

/**
 * В общем случае, вам нужно думать о методах этого интерфейса, как об UNDO/REDO (то бишь Ctrl+Z/Ctrl+Y).
 * onSuccess -- это REDO, onError -- это UNDO. Желательно сделать методы идемпотентными, то есть если кто-то
 * вызовет onSuccess несколько раз -- результат этих вызовов будет такой же, как если бы кто-то вызвал его ровно один раз.
 */
public abstract class RdmChangeDataRequestCallback {

    private static final Logger logger = LoggerFactory.getLogger(RdmChangeDataRequestCallback.class);

    @Autowired
    private VersionMappingService versionMappingService;

    @Autowired
    private RdmSyncDao dao;

    /**
     * Этот метод будет вызван, если изменения применились в RDM.
     */
    @Transactional
    public <T extends Serializable> void onSuccess(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {

        casState(refBookCode, addUpdate, RdmSyncLocalRowState.SYNCED);
        onSuccess0(refBookCode, addUpdate, delete);
    }

    protected abstract <T extends Serializable> void onSuccess0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete);

    /**
     * Этот метод будет вызван, если RDM вернул ошибку, не связанную с блокировками справочников или произошел таймаут соединения.
     * Таким образом, даже если ваши изменения могли пройти в RDM (то бишь по валидациям и т.п), но в RDM что - то пошло не так
     * (скажем произошел OutOfMemoryError) -- запись там не появится. Если ошибка таймаутовая -- будет вызван этот метод
     * (однако изменения могут как появится, так и нет).
     */
    @Transactional
    public <T extends Serializable> void onError(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Exception ex) {

        casState(refBookCode, addUpdate, RdmSyncLocalRowState.ERROR);
        casState(refBookCode, delete, RdmSyncLocalRowState.ERROR);
        onError0(refBookCode, addUpdate, delete, ex);
    }

    protected abstract <T extends Serializable> void onError0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Exception ex);

    private <T extends Serializable> void casState(String refBookCode, List<? extends T> addUpdate, RdmSyncLocalRowState state) {
        VersionMapping vm = versionMappingService.getVersionMapping(refBookCode, null);
        if (vm == null)
            return;

        boolean haveTrigger = dao.existsInternalLocalRowStateUpdateTrigger(vm.getTable());

        String pk = vm.getPrimaryField();
        String table = vm.getTable();
        List<Object> pks = RdmSyncDataUtils.extractSnakeCaseKey(pk, addUpdate);

        if (haveTrigger) {
            dao.disableInternalLocalRowStateUpdateTrigger(vm.getTable());
        }
        try {
            boolean stateChanged = dao.setLocalRecordsState(table, pk, pks, RdmSyncLocalRowState.PENDING, state);
            if (!stateChanged) {
                logger.info("State change did not pass. Skipping callback on {}.", refBookCode);
                throw new RdmSyncException();
            }
        } finally {
            if (haveTrigger) {
                dao.enableInternalLocalRowStateUpdateTrigger(vm.getTable());
            }

        }
    }

    public static class DefaultRdmChangeDataRequestCallback extends RdmChangeDataRequestCallback {

        private static final Logger logger = LoggerFactory.getLogger(DefaultRdmChangeDataRequestCallback.class);

        private static final String LOG_PULLED_SUCCESS = "Successfully pulled into RDM for refBook with code {}. " +
                "Payload:\nAdded/Update objects: {},\nDeleted objects: {}";
        private static final String LOG_PULLED_FAILURE = "Error occurred while pulling data into RDM for refBook with code {}. " +
                "Payload:\nattempt to Add/Update objects: {},\nattempt to Delete objects: {}";

        @Override
        public <T extends Serializable> void onSuccess0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
            logger.info(LOG_PULLED_SUCCESS, refBookCode, addUpdate, delete);
        }

        @Override
        public <T extends Serializable> void onError0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Exception ex) {
            logger.error(LOG_PULLED_FAILURE, refBookCode, addUpdate, delete, ex);
        }
    }
}
