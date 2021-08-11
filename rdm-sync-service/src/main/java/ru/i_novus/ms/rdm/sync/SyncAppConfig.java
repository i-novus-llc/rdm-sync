package ru.i_novus.ms.rdm.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import ru.i_novus.ms.rdm.sync.quartz.AutowiringSpringBeanJobFactory;

@Configuration
public class SyncAppConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public AutowiringSpringBeanJobFactory autowiringSpringBeanJobFactory() {
        return new AutowiringSpringBeanJobFactory(applicationContext);
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {

        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setJobFactory(autowiringSpringBeanJobFactory());

        return schedulerFactoryBean;
    }
}
