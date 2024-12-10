package ru.i_novus.ms.rdm.sync.event;

import org.springframework.context.ApplicationEvent;

/**
 * Событие завершения инициализации маппинга
 */
public class RdmSyncInitCompleteEvent extends ApplicationEvent {
    public RdmSyncInitCompleteEvent(Object source) {
        super(source);
    }
}
