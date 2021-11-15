package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

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

    @Value("#{${rdm_sync.auto_create.refbook_codes:}}")
    private Map<String,String> autoCreateRefBookCodes;

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

        if (autoCreateRefBookCodes == null)
            return;

        for (String refBookCode : autoCreateRefBookCodes) {

            localRefBookCreatorLocator.getLocalRefBookCreator(refBookCode).create(refBookCode, null);
        }
    }

    private void sourceLoaderServiceInit() {
        sourceLoaderServiceList.forEach(SourceLoaderService::load);
    }

}
