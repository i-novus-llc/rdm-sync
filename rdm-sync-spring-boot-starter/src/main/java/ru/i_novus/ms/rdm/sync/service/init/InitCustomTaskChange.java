package ru.i_novus.ms.rdm.sync.service.init;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ResourceAccessor;
import org.apache.cxf.common.util.ReflectionUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;

public class InitCustomTaskChange implements CustomTaskChange {

    private boolean skip;
    private RdmSyncInitializer initializer;

    @Override
    public void execute(final Database database) {
        if (skip) {
            return;
        }
        initializer.init();
    }

    @Override
    public String getConfirmationMessage() {
        return "RdmSyncInitCustomTaskChange";
    }

    @Override
    public void setUp() {
        return;
    }

    @Override
    public void setFileOpener(final ResourceAccessor resourceAccessor) {
        final SpringLiquibase liquibase = ReflectionUtil.accessDeclaredField(
            "this$0",
            SpringLiquibase.SpringResourceOpener.class,
            resourceAccessor,
            SpringLiquibase.class
        );
        final ApplicationContext context = (ApplicationContext) liquibase.getResourceLoader();
        final PropertyResolver propertyResolver = context.getBean(PropertyResolver.class);
        final String loaderInit = propertyResolver.getProperty("rdm-sync.auto_create.loader.enable");
        if (loaderInit != null && loaderInit.equalsIgnoreCase("true")) {
            skip = true;
            return;
        }
        initializer = context.getBean(RdmSyncInitializer.class);
    }

    @Override
    public ValidationErrors validate(final Database database) {
        return null;
    }

}
