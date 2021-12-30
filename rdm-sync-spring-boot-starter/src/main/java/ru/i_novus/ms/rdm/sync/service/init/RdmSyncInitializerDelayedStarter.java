package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name="rdm-sync.init.delay")
public class RdmSyncInitializerDelayedStarter {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializerDelayedStarter.class);

    @Autowired
    private RdmSyncConfigurer configurer;

    @PostConstruct
    public void start() {
        if (configurer != null){
            configurer.setupJobs();
        } else {
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
        }
    }
}
