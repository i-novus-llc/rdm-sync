package ru.i_novus.ms.rdm.sync.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.service.init.RdmSyncInitializer;

@Component
@DisallowConcurrentExecution
public final class RdmSyncInitJob implements Job {

    public static final String NAME = "RdmSyncInitialize";

    @Autowired
    private RdmSyncInitializer rdmSyncInitializer;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        rdmSyncInitializer.init();
    }

}
