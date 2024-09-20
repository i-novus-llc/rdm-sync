package ru.i_novus.ms.rdm.sync.fnsi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceDao;
import ru.i_novus.ms.rdm.sync.api.service.SourceLoaderService;

@Slf4j
public class MockFnsiSourceLoaderService implements SourceLoaderService {

    private final String url;

    private final String userKey;

    private final SyncSourceDao dao;

    @Autowired
    public MockFnsiSourceLoaderService(String url, String userKey, SyncSourceDao dao) {

        this.url = url;
        this.userKey = userKey;
        this.dao = dao;
    }

    @Override
    public void load() {

        final String initValues = String.format("{\"userKey\":\"%s\", \"url\":\"%s\"}", userKey, url);
        final SyncSource source = new SyncSource("MOCK-ФНСИ", "MOCK_FNSI", initValues,
                MockFnsiSyncSourceServiceFactory.class.getSimpleName());

        dao.save(source);

        log.info("Load mock source with\n name: {} \n code: {} \n init_values: {} \n service: {}",
                source.getName(), source.getCode(), source.getInitValues(), source.getFactoryName());
    }
}
