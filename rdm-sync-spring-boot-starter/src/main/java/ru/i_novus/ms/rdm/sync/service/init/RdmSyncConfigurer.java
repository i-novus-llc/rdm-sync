package ru.i_novus.ms.rdm.sync.service.init;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;
import ru.i_novus.ms.rdm.sync.quartz.RdmSyncExportDirtyRecordsToRdmJob;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
@ConditionalOnClass(name = "org.quartz.Scheduler")
@ConditionalOnProperty(name = "rdm_sync.scheduling", havingValue = "true")
class RdmSyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncConfigurer.class);

    private static final String LOG_SCHEDULER_NON_CLUSTERED =
            "Scheduler is configured in non clustered mode. There may be concurrency issues.";
    private static final String LOG_JOB_SCHEDULE_ERROR =
            "Cannot schedule %s job. All records in the %s state will remain in it.";
    private static final String LOG_TRIGGER_NOT_CHANGED = "Trigger's {} expression is not changed.";
    private static final String LOG_TRIGGER_IS_NOT_CRON = "Trigger {} is not CronTrigger instance. Leave it as it is.";

    private static final String JOB_GROUP = "RDM_SYNC_INTERNAL";

    @Autowired(required = false)
    private Scheduler scheduler;

    @Autowired
    private ClusterLockService clusterLockService;

    @Value("${rdm_sync.export.to_rdm.cron:0/5 * * * * ?}")
    private String exportToRdmCron;

    @Value("${rdm_sync.change_data.mode:null}")
    private String changeDataMode;

    @Transactional
    public void setupJobs() {

        if (scheduler == null) return;

        if (!clusterLockService.tryLock()) return;

        setupExportJob();
    }

    private void setupExportJob() {

        if (StringUtils.isEmpty(exportToRdmCron))
            return;

        final String jobName = RdmSyncExportDirtyRecordsToRdmJob.NAME;
        try {
            if (!scheduler.getMetaData().isJobStoreClustered()) {
                logger.warn(LOG_SCHEDULER_NON_CLUSTERED);
            }

            JobKey jobKey = JobKey.jobKey(jobName, JOB_GROUP);
            if (changeDataMode == null) {
                deleteJob(jobKey);
                return;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            Trigger oldTrigger = scheduler.getTrigger(triggerKey);

            JobDetail newJob = newJob(RdmSyncExportDirtyRecordsToRdmJob.class).
                    withIdentity(jobKey).
                    build();
            Trigger newTrigger = newTrigger().
                    withIdentity(triggerKey).
                    forJob(newJob).
                    withSchedule(CronScheduleBuilder.cronSchedule(exportToRdmCron)).
                    build();

            addJob(triggerKey, oldTrigger, newJob, newTrigger, exportToRdmCron);

        } catch (SchedulerException e) {

            String message = String.format(LOG_JOB_SCHEDULE_ERROR, jobName, RdmSyncLocalRowState.DIRTY);
            logger.error(message, e);
        }
    }

    private void deleteJob(JobKey jobKey) throws SchedulerException {

        if (scheduler.checkExists(jobKey)) {

            scheduler.deleteJob(jobKey);
        }
    }

    private void addJob(TriggerKey triggerKey, Trigger oldTrigger,
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
}
