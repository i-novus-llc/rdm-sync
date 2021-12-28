package ru.i_novus.ms.rdm.sync.service.init;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.quartz.RdmSyncInitJob;

import javax.annotation.PostConstruct;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class RdmSyncInitializerConfigurer extends BaseRdmSyncConfigurer{

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializerConfigurer.class);

    @Value("${rdm-sync.init.delay:0}")
    private Integer rdmSyncInitDelay;

    @Override
    @Transactional
    @PostConstruct
    public void setupJobs() {

        if (scheduler == null)
            return;

        setupRdmSyncInitJob();

    }

    private void setupRdmSyncInitJob(){

        final String jobName = RdmSyncInitJob.NAME;

        try {
            if (!scheduler.getMetaData().isJobStoreClustered()) {
                logger.warn(LOG_SCHEDULER_NON_CLUSTERED);
            }

            JobKey jobKey = JobKey.jobKey(jobName, JOB_GROUP);
            if (isEmpty(rdmSyncInitDelay)) {
                deleteJob(jobKey);
                return;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            Trigger oldTrigger = scheduler.getTrigger(triggerKey);

            JobDetail newJob = newJob(RdmSyncInitJob.class)
                    .withIdentity(jobKey)
                    .build();
            Trigger newTrigger = newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(newJob)
                    .startAt(nowDelayed(rdmSyncInitDelay))
                    .build();

            addJob(triggerKey, oldTrigger, newJob, newTrigger, String.valueOf(rdmSyncInitDelay));

        } catch (SchedulerException e) {
            String message = String.format(LOG_JOB_CANNOT_SCHEDULE, jobName);
            logger.error(message, e);
        }

    }

}
