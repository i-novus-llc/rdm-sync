package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionAndFieldMapping;
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
        autoCreate(getVersionAndFieldMappings());

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

    private void autoCreate(List<VersionAndFieldMapping> versionAndFieldMappings) {

        versionAndFieldMappings.stream()
                .sorted(Comparator.comparingInt(VersionAndFieldMapping::getMappingVersion).reversed())
                .forEach(versionAndFieldMapping ->
                        localRefBookCreatorLocator.getLocalRefBookCreator(getSyncType(versionAndFieldMapping))
                .create(versionAndFieldMapping));
    }

    private SyncTypeEnum getSyncType(VersionAndFieldMapping versionAndFieldMapping) {
        return versionAndFieldMapping.getVersionMapping().getType();
    }

    private List<VersionAndFieldMapping> getVersionAndFieldMappings() {
        return mappingSourceServiceList.stream()
                .map(MappingSourceService::getVersionAndFieldMappingList)
                .reduce(new ArrayList<>(), (versionAndFieldMappingListFromXml, versionAndFieldMappingListFromProperties) ->
                {
                    List<VersionAndFieldMapping> result = new ArrayList<>();
                    result.addAll(versionAndFieldMappingListFromXml);
                    result.addAll(versionAndFieldMappingListFromProperties);
                    return result;
                });
    }

}
