package ru.i_novus.ms.rdm.sync.service.init;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.integration.spring.SpringResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.apache.cxf.common.util.ReflectionUtil;
import org.springframework.context.ApplicationContext;

public class InitCustomTaskChange implements CustomTaskChange {

    private RdmSyncInitializer initializer;

    @Override
    public void execute(final Database database) {
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
            SpringResourceAccessor.class,
            resourceAccessor,
            SpringLiquibase.class
        );

        final ApplicationContext context = (ApplicationContext) liquibase.getResourceLoader();
        initializer = context.getBean(RdmSyncInitializer.class);
    }

    @Override
    public ValidationErrors validate(final Database database) {
        return null;
    }

}
