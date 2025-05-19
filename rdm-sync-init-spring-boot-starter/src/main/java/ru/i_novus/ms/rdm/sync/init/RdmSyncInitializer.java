package ru.i_novus.ms.rdm.sync.init;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.mapping.MappingRangeValidator;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.init.dao.VersionMappingDao;
import ru.i_novus.ms.rdm.sync.init.event.RdmSyncInitCompleteEvent;
import ru.i_novus.ms.rdm.sync.init.loader.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.init.mapping.MappingSourceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@DependsOn("rdmSyncLiquibaseService")
@ConditionalOnProperty(value = "rdm-sync.init.enabled", havingValue ="true", matchIfMissing = true)
public class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired
    private SourceLoaderService sourceLoaderService;

    @Autowired
    private List<MappingSourceService> mappingSourceServiceList;

    @Autowired
    private LocalRefBookCreatorLocator localRefBookCreatorLocator;

    @Autowired
    private VersionMappingDao versionMappingDao;

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostConstruct
    public void init() {

        sourceLoaderServiceInit();
        List<SyncMapping> syncMappings = getSyncMappings();
        autoCreate(syncMappings);
        deleteNotActualMappings(syncMappings);
        publisher.publishEvent(new RdmSyncInitCompleteEvent(this));
        logger.info("initialization finished");
    }

    private void deleteNotActualMappings(List<SyncMapping> syncMappings) {
        List<VersionMapping> vmFromDb = versionMappingDao.getVersionMappings();
        Set<VersionMapping> versionMappingsForSync = syncMappings.stream()
                .map(SyncMapping::getVersionMapping)
                .collect(Collectors.toSet());

        Set<Integer> mappingIdsToDelete = vmFromDb.stream()
                .filter(versionMappingFromDb -> versionMappingsForSync.stream().noneMatch(versionMappingFromDb::equalsByRange))
                .map(VersionMapping::getMappingId).collect(Collectors.toSet());
        logger.info("delete {} not actual mappings", mappingIdsToDelete.size());
        if (!mappingIdsToDelete.isEmpty()) {
            versionMappingDao.deleteVersionMappings(mappingIdsToDelete);
        }
    }

    private void sourceLoaderServiceInit() {
        sourceLoaderService.load();
    }

    private void autoCreate(List<SyncMapping> syncMappings) {
        MappingRangeValidator.validate(syncMappings);
        syncMappings.stream()
                .sorted(new SyncMappingComparator())
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
