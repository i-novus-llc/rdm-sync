package ru.i_novus.ms.rdm.sync.quartz;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import jakarta.annotation.Nonnull;

public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

    private final AutowireCapableBeanFactory beanFactory;

    public AutowiringSpringBeanJobFactory(final ApplicationContext context) {

        this.beanFactory = context.getAutowireCapableBeanFactory();
    }

    @Override
    @Nonnull
    protected Object createJobInstance(@Nonnull TriggerFiredBundle bundle) throws Exception {

        Object job = super.createJobInstance(bundle);
        beanFactory.autowireBean(job);

        return job;
    }
}
