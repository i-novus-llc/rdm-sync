package ru.i_novus.ms.rdm.sync.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.service.RdmSyncService;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;

@Component
@DisallowConcurrentExecution
public final class RdmSyncImportRecordsFromRdmJob implements Job {

    public static final String NAME = "ImportRecordsFromRdm";

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private RdmSyncService rdmSyncService;

    @Autowired
    private List<SourceLoaderService> sourceLoaderServiceList;

    @Override
    public void execute(JobExecutionContext context) {
        sourceLoaderServiceList.forEach(SourceLoaderService::load);
        rdmSyncService.update();
    }
}
