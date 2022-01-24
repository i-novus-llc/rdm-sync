package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.List;

@Component
@DependsOn("liquibaseRdm")
public class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired
    private XmlMappingLoaderService mappingLoaderService;

    @Autowired
    private List<SourceLoaderService> sourceLoaderServiceList;

    @Autowired
    private RdmSyncDao dao;

    @Autowired(required = false)
    private RdmSyncJobConfigurer rdmSyncJobConfigurer;

    @Autowired
    private LocalRefBookCreatorLocator localRefBookCreatorLocator;

    @Autowired
    private AutoCreateRefBookProperty autoCreateRefBookProperties;

    @Value("${rdm-sync.auto_create.loader.enable:false}")
    private boolean loaderInit;

    public void init() {

        if (loaderInit) {
            sourceLoaderServiceInit();
            mappingLoaderService.load();
        }

        autoCreate();

        if (rdmSyncJobConfigurer != null) {
            rdmSyncJobConfigurer.setupImportJob();
            rdmSyncJobConfigurer.setupExportJob();
        } else
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
    }

    private void autoCreate() {

        if (autoCreateRefBookProperties == null || autoCreateRefBookProperties.getRefbooks() == null)
            return;

        autoCreateRefBookProperties.getRefbooks().forEach(p ->
            localRefBookCreatorLocator.getLocalRefBookCreator(p.getType()).create(p.getCode(), p.getName(), p.getSource(), p.getType(), p.getTable())
        );

    }

    private void sourceLoaderServiceInit() {
        sourceLoaderServiceList.forEach(SourceLoaderService::load);
    }
}
