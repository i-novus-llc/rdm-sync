package ru.i_novus.ms.rdm.sync.service.change_data;

import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.jaxrs.RestException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class RdmChangeDataListener {

    private static final Set<String> CONCURRENCY_ISSUES = Set.of("refbook.lock.draft.is.publishing", "refbook.lock.draft.is.updating", "refbook.lock.cannot-be-acquired");

    private final RestClient restClient;

    private final RdmChangeDataRequestCallback callback;

    public RdmChangeDataListener(String url,
                                 RdmChangeDataRequestCallback callback) {
        this.restClient = RestClient.create(url);
        this.callback = callback;
    }

    @JmsListener(destination = "${rdm-sync.change_data.queue:rdmChangeData}",
            containerFactory = "rdmChangeDataQueueMessageListenerContainerFactory")
    public <T extends Serializable> void onChangeDataRequest(List<Object> msg) {

        final List<List<? extends T>> src = (List<List<? extends T>>) msg.get(0);
        final List<? extends T> addUpdate = src.get(0);
        final List<? extends T> delete = src.get(1);

        final Map<String, Object> request = (Map<String, Object>) msg.get(1);
        final String refBookCode = (String) request.get("refBookCode");

        log.info("Change data request on refBook with code {} arrived.", refBookCode);
        try {
            changeDataByRequest(refBookCode, request);

            callback.onSuccess(refBookCode, addUpdate, delete);

        } catch (RestException re) {
            boolean concurrencyIssue = false;
            if (re.getErrors() != null) {
                concurrencyIssue = re.getErrors().stream()
                        .filter(error -> error != null && error.getMessage() != null)
                        .anyMatch(error -> CONCURRENCY_ISSUES.contains(error.getMessage()));
            }
            if (re.getMessage() != null) {
                concurrencyIssue |= CONCURRENCY_ISSUES.contains(re.getMessage());
            }

            if (concurrencyIssue)
                throw new RdmSyncException();

            callback.onError(refBookCode, addUpdate, delete, re);

        } catch (Exception e) {
            log.error("Error occurred while pulling changes into RDM. No redelivery.", e);
            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }

    // to-do: Копия из changeDataByRequest, выделить в отдельный сервис.
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
