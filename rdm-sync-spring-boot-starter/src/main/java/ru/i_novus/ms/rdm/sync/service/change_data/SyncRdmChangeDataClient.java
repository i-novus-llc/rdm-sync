package ru.i_novus.ms.rdm.sync.service.change_data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class SyncRdmChangeDataClient extends RdmChangeDataClient {

    private final RestClient restClient;

    public SyncRdmChangeDataClient(String url) {
        this.restClient = RestClient.create(url);
    }

    public <T extends Serializable> void changeData0(
            String refBookCode,
            List<? extends T> addUpdate,
            List<? extends T> delete,
            Function<? super T, Map<String, Object>> map
    ) {
        final Map<String, Object> request = toRdmChangeDataRequest(refBookCode, addUpdate, delete, map);
        try {
            changeDataByRequest(refBookCode, request);

            callback.onSuccess(refBookCode, addUpdate, delete);

        } catch (Exception e) {

            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }

    private void changeDataByRequest(String code, Map<String, Object> request) {

        final String uri = "/refBook/changeData";
        try {
            restClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error change data of refBook [{}]", code, e);
            throw new RuntimeException("Failed to get refBook", e);
        }
    }
}
