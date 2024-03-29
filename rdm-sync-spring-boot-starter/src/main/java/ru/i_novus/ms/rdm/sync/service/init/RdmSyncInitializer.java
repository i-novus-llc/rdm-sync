package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;
import ru.i_novus.ms.rdm.sync.service.mapping.MappingSourceService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Component
@DependsOn("liquibaseRdm")
public class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired(required = false)
    private RdmSyncJobConfigurer rdmSyncJobConfigurer;

    @Autowired
    private List<SourceLoaderService> sourceLoaderServiceList;

    @Autowired
    private List<MappingSourceService> mappingSourceServiceList;

    @Autowired
    private LocalRefBookCreatorLocator localRefBookCreatorLocator;

    public void init() {

        sourceLoaderServiceInit();
        autoCreate(getSyncMappings());

        if (rdmSyncJobConfigurer != null) {
            rdmSyncJobConfigurer.setupImportJob();
            rdmSyncJobConfigurer.setupExportJob();
        } else {
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. " +
                    "Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
        }

    }

    private void sourceLoaderServiceInit() {
        sourceLoaderServiceList.forEach(SourceLoaderService::load);
    }

    private void autoCreate(List<SyncMapping> syncMappings) {

        syncMappings.stream()
                .sorted(Comparator.comparingInt(SyncMapping::getMappingVersion).reversed())
                .forEach(syncMapping ->
                        localRefBookCreatorLocator.getLocalRefBookCreator(getSyncType(syncMapping))
                .create(syncMapping));
    }

    private SyncTypeEnum getSyncType(SyncMapping syncMapping) {
        return syncMapping.getVersionMapping().getType();
    }

    private List<SyncMapping> getSyncMappings() {
        return mappingSourceServiceList.stream()
                .map(MappingSourceService::getMappings)
                .reduce(new ArrayList<>(), (syncMappings1, syncMappings2) ->
                {
                    List<SyncMapping> result = new ArrayList<>();
                    result.addAll(syncMappings1);
                    result.addAll(syncMappings2);
                    return result;
                });
    }

}
