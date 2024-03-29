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
import ru.i_novus.ms.rdm.sync.quartz.RdmSyncExportDirtyRecordsToRdmJob;
import ru.i_novus.ms.rdm.sync.quartz.RdmSyncImportRecordsFromRdmJob;
import ru.i_novus.ms.rdm.sync.quartz.RdmSyncInitJob;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.Date;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.springframework.util.StringUtils.isEmpty;

@Component
@ConditionalOnClass(name = "org.quartz.Scheduler")
@ConditionalOnProperty(name = "rdm-sync.scheduling", havingValue = "true")
class RdmSyncJobConfigurer {

    private final Logger logger = LoggerFactory.getLogger(RdmSyncJobConfigurer.class);

    private static final String LOG_SCHEDULER_NON_CLUSTERED =
            "Scheduler is configured in non clustered mode. There is may be concurrency issues.";
    private static final String LOG_TRIGGER_NOT_CHANGED = "Trigger's {} expression is not changed.";
    private static final String LOG_TRIGGER_IS_NOT_CRON = "Trigger {} is not CronTrigger instance. Leave it as it is.";
    private static final String LOG_JOB_CANNOT_SCHEDULE = "Cannot schedule %s job.";

    private static final String JOB_GROUP = "RDM_SYNC_INTERNAL";

    private static final String LOG_ALL_RECORDS_WILL_REMAIN = "All records in the %s state will remain the same.";

    @Autowired(required = false)
    protected Scheduler scheduler;

    @Autowired
    private ClusterLockService clusterLockService;

    @Value("${rdm-sync.init.delay:#{null}}")
    private Integer rdmSyncInitDelay;

    @Value("${rdm-sync.import.from_rdm.cron:}")
    private String importFromRdmCron;

    @Value("${rdm-sync.import.from_rdm.delay:0}")
    private Integer importFromRdmDelay;

    @Value("${rdm-sync.export.to_rdm.cron:0/5 * * * * ?}")
    private String exportToRdmCron;

    @Value("${rdm-sync.export.to_rdm.delay:0}")
    private Integer exportToRdmDelay;

    @Value("${rdm-sync.change_data.mode:#{null}}")
    private String changeDataMode;

    @Transactional
    public void setupJobs() {

        if (scheduler == null)
            return;
        setupRdmSyncInitJob();

    }

    @Transactional
    public void setupImportJob() {

        final String jobName = RdmSyncImportRecordsFromRdmJob.NAME;
        try {
            if (!scheduler.getMetaData().isJobStoreClustered()) {
                logger.warn(LOG_SCHEDULER_NON_CLUSTERED);
            }

            JobKey jobKey = JobKey.jobKey(jobName, JOB_GROUP);
            if (isEmpty(importFromRdmCron)) {
                deleteJob(jobKey);
                return;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            Trigger oldTrigger = scheduler.getTrigger(triggerKey);

            JobDetail newJob = newJob(RdmSyncImportRecordsFromRdmJob.class)
                    .withIdentity(jobKey)
                    .build();
            Trigger newTrigger = newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(newJob)
                    .withSchedule(CronScheduleBuilder.cronSchedule(importFromRdmCron))
                    .startAt(nowDelayed(importFromRdmDelay))
                    .build();

            addJob(triggerKey, oldTrigger, newJob, newTrigger, importFromRdmCron);

        } catch (SchedulerException e) {

            String message = String.format(LOG_JOB_CANNOT_SCHEDULE, jobName);
            logger.error(message, e);
        }
    }

    @Transactional
    public void setupExportJob() {

        if (!clusterLockService.tryLock()) return;

        final String jobName = RdmSyncExportDirtyRecordsToRdmJob.NAME;
        try {
            if (!scheduler.getMetaData().isJobStoreClustered()) {
                logger.warn(LOG_SCHEDULER_NON_CLUSTERED);
            }

            JobKey jobKey = JobKey.jobKey(jobName, JOB_GROUP);
            if (isEmpty(exportToRdmCron) || isEmpty(changeDataMode)) {
                deleteJob(jobKey);
                return;
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            Trigger oldTrigger = scheduler.getTrigger(triggerKey);

            JobDetail newJob = newJob(RdmSyncExportDirtyRecordsToRdmJob.class)
                    .withIdentity(jobKey)
                    .build();
            Trigger newTrigger = newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(newJob)
                    .withSchedule(CronScheduleBuilder.cronSchedule(exportToRdmCron))
                    .startAt(nowDelayed(exportToRdmDelay))
                    .build();

            addJob(triggerKey, oldTrigger, newJob, newTrigger, exportToRdmCron);

        } catch (SchedulerException e) {

            String message = String.format(LOG_JOB_CANNOT_SCHEDULE, jobName) + "\n" +
                    String.format(LOG_ALL_RECORDS_WILL_REMAIN, RdmSyncLocalRowState.DIRTY);
            logger.error(message, e);
        }
    }

    private void setupRdmSyncInitJob() {

        final String jobName = RdmSyncInitJob.NAME;

        try {
            if (!scheduler.getMetaData().isJobStoreClustered()) {
                logger.warn(LOG_SCHEDULER_NON_CLUSTERED);
            }

            JobKey jobKey = JobKey.jobKey(jobName, JOB_GROUP);

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

    private Date nowDelayed(Integer delayMillis) {
        return new Date(System.currentTimeMillis() + (delayMillis != null ? delayMillis.longValue() : 0));
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

    private void deleteJob(JobKey jobKey) throws SchedulerException {

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

}
