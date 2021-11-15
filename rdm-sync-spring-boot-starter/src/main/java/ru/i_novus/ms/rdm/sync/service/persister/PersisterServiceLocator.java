package ru.i_novus.ms.rdm.sync.service.persister;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PersisterServiceLocator {

    private final Set<PersisterService> persisterServices;

    public PersisterServiceLocator(Set<PersisterService> persisterServices) {
        this.persisterServices = persisterServices;
    }

    public PersisterService getPersisterService(String refBookCode) {
        return persisterServices.iterator().next();
    }
}
