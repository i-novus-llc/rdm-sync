package ru.i_novus.ms.rdm.sync.service.init;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalRefBookCreatorLocator {

    private final List<LocalRefBookCreator> localRefBookCreators;

    public LocalRefBookCreatorLocator(List<LocalRefBookCreator> localRefBookCreators) {
        this.localRefBookCreators = localRefBookCreators;
    }

    LocalRefBookCreator getLocalRefBookCreator(String refBookCode) {
        return localRefBookCreators.get(0);
    }


}
