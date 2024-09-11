package ru.i_novus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AsyncRdmChangeDataClient extends RdmChangeDataClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRdmChangeDataClient.class);

    private final JmsTemplate jmsTemplate;

    private final String rdmChangeDataQueue;

    public AsyncRdmChangeDataClient(JmsTemplate jmsTemplate,
                                    String rdmChangeDataQueue) {
        this.jmsTemplate = jmsTemplate;
        this.rdmChangeDataQueue = rdmChangeDataQueue;
    }

    @Override
    public <T extends Serializable> void changeData0(
            String refBookCode,
            List<? extends T> addUpdate,
            List<? extends T> delete,
            Function<? super T, Map<String, Object>> map
    ) {
        try {
            jmsTemplate.convertAndSend(
                    rdmChangeDataQueue,
                    List.of(
                            Arrays.asList(addUpdate, delete),
                            toRdmChangeDataRequest(refBookCode, addUpdate, delete, map)
                    )
            );
        } catch (Exception e) {
            logger.error("An error occurred while sending message to the message broker.", e);

            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }
}