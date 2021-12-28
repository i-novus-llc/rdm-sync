package ru.i_novus.ms.rdm.sync.service.init;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public abstract class BaseRdmSyncConfigurer {

    protected final Logger logger = LoggerFactory.getLogger(BaseRdmSyncConfigurer.class);

    protected static final String LOG_SCHEDULER_NON_CLUSTERED =
            "Scheduler is configured in non clustered mode. There is may be concurrency issues.";
    protected static final String LOG_TRIGGER_NOT_CHANGED = "Trigger's {} expression is not changed.";
    protected static final String LOG_TRIGGER_IS_NOT_CRON = "Trigger {} is not CronTrigger instance. Leave it as it is.";
    protected static final String LOG_JOB_CANNOT_SCHEDULE = "Cannot schedule %s job.";

    protected static final String JOB_GROUP = "RDM_SYNC_INTERNAL";

    @Autowired(required = false)
    protected Scheduler scheduler;

    public abstract void setupJobs();

    protected Date nowDelayed(Integer delayMillis) {
        return new Date(System.currentTimeMillis() + (delayMillis != null ? delayMillis.longValue() : 0));
    }

    protected void addJob(TriggerKey triggerKey, Trigger oldTrigger,
                        JobDetail newJob, Trigger newTrigger, String cronExpression) throws SchedulerException {

        if (oldTrigger == null) {

            scheduler.scheduleJob(newJob, newTrigger);

            return;
        }

        if (oldTrigger instanceof CronTrigger) {

            CronTrigger trigger = (CronTrigger) oldTrigger;
            if (!trigger.getCronExpression().equals(cronExpression)) {

                scheduler.rescheduleJob(triggerKey, newTrigger);

            } else
                logger.info(LOG_TRIGGER_NOT_CHANGED, triggerKey);

        } else {
            logger.warn(LOG_TRIGGER_IS_NOT_CRON, triggerKey);
        }
    }

    protected void deleteJob(JobKey jobKey) throws SchedulerException {

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

}
