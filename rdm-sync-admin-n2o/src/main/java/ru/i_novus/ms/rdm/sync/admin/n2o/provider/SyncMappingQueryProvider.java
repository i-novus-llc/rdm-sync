package ru.i_novus.ms.rdm.sync.admin.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.admin.n2o.model.SyncMappingRequest;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Провайдер для формирования запроса на получение данных
 * по конкретному маппингу полей версии справочника.
 */
@Service
public class SyncMappingQueryProvider extends SyncMappingBaseProvider implements DynamicMetadataProvider {

    static final String QUERY_PROVIDER_ID = "syncMappingQuery";

    @Override
    public String getCode() {
        return QUERY_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        SyncMappingRequest request = toRequest(context);
        return singletonList(createQuery(request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oQuery.class);
    }

    private N2oQuery createQuery(SyncMappingRequest request) {

        N2oQuery n2oQuery = new N2oQuery();
        //n2oQuery.setUniques(new N2oQuery.Selection[]{ createSelection() });
        //n2oQuery.setFields(createQueryFields(request));

        return n2oQuery;
    }
}
