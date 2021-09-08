package ru.i_novus.ms.rdm.sync.admin.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.admin.n2o.model.SyncMappingRequest;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Провайдер для формирования объекта по выполнению операции
 * создания/изменения маппинга полей версии справочника.
 */
@Service
public class SyncMappingObjectProvider extends SyncMappingBaseProvider implements DynamicMetadataProvider {

    static final String OBJECT_PROVIDER_ID = "syncMappingObject";

    @Override
    public String getCode() {
        return OBJECT_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        SyncMappingRequest request = toRequest(context);
        return singletonList(createObject(request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oObject.class);
    }

    private N2oObject createObject(SyncMappingRequest request) {

        N2oObject n2oObject = new N2oObject();
        //n2oObject.setOperations(createOperations(request));

        return n2oObject;
    }
}
