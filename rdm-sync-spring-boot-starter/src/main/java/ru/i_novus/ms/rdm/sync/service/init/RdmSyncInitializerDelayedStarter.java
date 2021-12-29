package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name="rdm-sync.init.delay", havingValue = "")
public class RdmSyncInitializerDelayedStarter {

    @Autowired
    private RdmSyncInitializerConfigurer initializerConfigurer;

    @PostConstruct
    public void start() {
        initializerConfigurer.setupJobs();
    }
}
