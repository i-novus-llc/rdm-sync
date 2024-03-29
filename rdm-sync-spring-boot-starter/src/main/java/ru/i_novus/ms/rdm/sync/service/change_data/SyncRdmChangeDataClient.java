package ru.i_novus.ms.rdm.sync.service.change_data;

import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SyncRdmChangeDataClient extends RdmChangeDataClient {

    private final RefBookService refBookService;

    public SyncRdmChangeDataClient(RefBookService refBookService) {

        this.refBookService = refBookService;
    }

    public <T extends Serializable> void changeData0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Function<? super T, Map<String, Object>> map) {

        RdmChangeDataRequest req = toRdmChangeDataRequest(refBookCode, addUpdate, delete, map);
        try {
            refBookService.changeData(req);

            callback.onSuccess(refBookCode, addUpdate, delete);

        } catch (Exception e) {

            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }
}
