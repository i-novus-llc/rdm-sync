package ru.i_novus.ms.rdm.sync.rdm;

import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.impl.RdmSyncSourceServiceFactory;

public class MockRdmSyncSourceServiceFactory extends RdmSyncSourceServiceFactory {

    public MockRdmSyncSourceServiceFactory(RestClient.Builder builder) {
        super(builder);
    }

    @Override
    public boolean isSatisfied(SyncSource source) {
        return source.getFactoryName().equals("MockRdmSyncSourceServiceFactory");
    }
}
