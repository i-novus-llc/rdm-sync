package ru.i_novus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.sync.AutoCreateRefBookProperty;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.List;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@Component
@DependsOn("liquibaseRdm")
public class RdmSyncInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncInitializer.class);

    @Autowired
    private XmlMappingLoaderService xmlMappingLoaderService;

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

    public void init() {

        sourceLoaderServiceInit();
        List<VersionMapping> versionMappings = xmlMappingLoaderService.load();
        autoCreate(versionMappings);

        if (rdmSyncJobConfigurer != null) {
            rdmSyncJobConfigurer.setupImportJob();
            rdmSyncJobConfigurer.setupExportJob();
        } else {
            logger.warn("Quartz scheduler is not configured. All records in the {} state will remain in it. " +
                    "Please, configure Quartz scheduler in clustered mode.", RdmSyncLocalRowState.DIRTY);
        }

    }

    private void autoCreate(List<VersionMapping> xmlMappings) {
        boolean createOnXml = !isEmpty(xmlMappings);
        boolean createOnProperties = !createOnXml &&
                autoCreateRefBookProperties != null && !isEmpty(autoCreateRefBookProperties.getRefbooks());

        if (createOnXml) {
            xmlMappings.forEach(m ->
                    autoCreate(m.getCode(), m.getRefBookName(), m.getSource(), m.getType(), m.getTable(), m.getRange()));
        }

        if (createOnProperties) {
            autoCreateRefBookProperties.getRefbooks().forEach(m ->
                    autoCreate(m.getCode(), m.getName(), m.getSource(), m.getType(), m.getTable(), m.getRange()));
        }
    }

    private void autoCreate(String refCode, String refName, String source, SyncTypeEnum type, String table, String range) {
        localRefBookCreatorLocator.getLocalRefBookCreator(type).create(refCode, refName, source, type, table, range);
    }

    private void sourceLoaderServiceInit() {
        sourceLoaderServiceList.forEach(SourceLoaderService::load);
    }
}
