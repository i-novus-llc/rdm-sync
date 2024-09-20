package ru.i_novus.ms.rdm.sync.fnsi;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.fnsi.sync.impl.FnsiSyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;

public class MockFnsiSyncSourceServiceFactory extends FnsiSyncSourceServiceFactory {

    public MockFnsiSyncSourceServiceFactory(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("MockFnsiSyncSourceServiceFactory");
    }
}
