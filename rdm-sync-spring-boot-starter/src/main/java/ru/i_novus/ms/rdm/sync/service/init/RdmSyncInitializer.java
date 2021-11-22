package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@DependsOn("liquibaseRdm")
class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired
    private XmlMappingLoaderService mappingLoaderService;

    @Autowired
    private List<SourceLoaderService> sourceLoaderServiceList;

    @Autowired
    private RdmSyncDao dao;

    @Autowired(required = false)
    private RdmSyncConfigurer rdmSyncConfigurer;

    @Autowired
    private LocalRefBookCreatorLocator localRefBookCreatorLocator;

    @Autowired
    private AutoCreateRefBookProperty autoCreateRefBookProperties;

    @PostConstruct
    public void start() {

        sourceLoaderServiceInit();
        mappingLoaderService.load();
        autoCreate();

        if (rdmSyncConfigurer != null) {
            rdmSyncConfigurer.setupJobs();
        } else
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
    }

    private void autoCreate() {

        if (autoCreateRefBookProperties == null)
            return;

        autoCreateRefBookProperties.getRefbooks().forEach(p ->
            localRefBookCreatorLocator.getLocalRefBookCreator(p.getType()).create(p.getName(), p.getCode())
        );

    }

    private void sourceLoaderServiceInit() {
        sourceLoaderServiceList.forEach(SourceLoaderService::load);
    }
}
