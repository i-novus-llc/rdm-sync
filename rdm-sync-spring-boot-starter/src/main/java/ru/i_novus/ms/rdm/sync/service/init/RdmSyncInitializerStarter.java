package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Стартер для инициализации лоадеров сразу после старта приложения
 */
@Component
@ConditionalOnProperty(name = "rdm-sync.init.delay", matchIfMissing = true, havingValue = "none")
public class RdmSyncInitializerStarter {

    @Autowired
    RdmSyncInitializer initializer;

    @PostConstruct
    public void start() {
            initializer.init();
    }
}
