package ru.i_novus.ms.rdm.sync.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;
import ru.i_novus.ms.rdm.sync.service.change_data.RdmChangeDataClient;

import java.util.*;

import static ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils.INTERNAL_TAG;
import static ru.i_novus.ms.rdm.sync.util.RdmSyncDataUtils.reindex;

@Component
@DisallowConcurrentExecution
public final class RdmSyncExportDirtyRecordsToRdmJob implements Job {

    public static final String NAME = "ExportDirtyRecordsToRdm";

    @Autowired
    private RdmSyncDao dao;

    @Autowired(required = false)
    private RdmChangeDataClient rdmChangeDataClient;

    @Value("${rdm_sync.export.to_rdm.batch_size:100}")
    private int exportToRdmBatchSize;

    @Override
    public void execute(JobExecutionContext context) {

        if (rdmChangeDataClient == null)
            return;

        final int limit = exportToRdmBatchSize;

        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping vm : versionMappings) {
            int offset = 0;
            String table = vm.getTable();
            List<FieldMapping> fieldMappings = dao.getFieldMappings(vm.getCode());
            String deletedKey = vm.getDeletedField();
            for (;;) {
                Page<Map<String, Object>> dirtyBatch = dao.getData(table, vm.getPrimaryField(), limit, offset, RdmSyncLocalRowState.DIRTY, null);
                if (dirtyBatch.getContent().isEmpty())
                    break;

                List<HashMap<String, Object>> addUpdate = new ArrayList<>();
                List<HashMap<String, Object>> delete = new ArrayList<>();
                for (Map<String, Object> map : dirtyBatch.getContent()) {
                    Boolean isDeleted = (Boolean) map.get(deletedKey);
                    if (Boolean.TRUE.equals(isDeleted))
                        delete.add((HashMap<String, Object>) map);
                    else
                        addUpdate.add((HashMap<String, Object>) map);
                }
                addUpdate.add(INTERNAL_TAG);

                rdmChangeDataClient.changeData(vm.getCode(), addUpdate, delete, record -> {
                    Map<String, Object> map = new HashMap<>(record);
                    reindex(fieldMappings, map);
                    return map;
                });
                offset += limit;
            }
        }
    }
}
