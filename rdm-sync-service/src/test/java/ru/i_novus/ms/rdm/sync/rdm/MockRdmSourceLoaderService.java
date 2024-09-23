package ru.i_novus.ms.rdm.sync.rdm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

@Slf4j
public class MockRdmSourceLoaderService implements SourceLoaderService {

    private final String url;

    private final SyncSourceDao dao;

    @Autowired
    public MockRdmSourceLoaderService(String url, SyncSourceDao dao) {

        this.url = url;
        this.dao = dao;
    }

    @Override
    public void load() {

        final SyncSource source = new SyncSource("MOCK-НСИ", "MOCK_RDM", url,
                MockRdmSyncSourceServiceFactory.class.getSimpleName());

        dao.save(source);

        log.info("Load mock source with\n name: {} \n code: {} \n init_values: {} \n service: {}",
                source.getName(), source.getCode(), source.getInitValues(), source.getFactoryName());
    }
}
