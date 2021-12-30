package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Стартер для инициализации лоадеров с таймером
 */
@Component
@ConditionalOnProperty(name = "rdm-sync.init.delay")
public class RdmSyncInitializerDelayedStarter {

    @Autowired
    RdmSyncJobConfigurer configurer;

    @PostConstruct
    public void start() {
            configurer.setupJobs();
    }
}
