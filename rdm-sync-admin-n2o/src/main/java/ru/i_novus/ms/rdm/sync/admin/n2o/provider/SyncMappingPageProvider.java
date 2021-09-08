package ru.i_novus.ms.rdm.sync.admin.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.global.view.page.N2oPage;
import net.n2oapp.framework.api.metadata.global.view.page.N2oSimplePage;
import net.n2oapp.framework.api.metadata.global.view.widget.N2oForm;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.admin.n2o.model.SyncMappingRequest;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Провайдер для формирования страницы по отображению данных
 * по созданию/изменению маппинга полей версии справочника.
 */
@Service
public class SyncMappingPageProvider extends SyncMappingBaseProvider implements DynamicMetadataProvider {

    static final String PAGE_PROVIDER_ID = "syncMappingPage";

    @Override
    public String getCode() {
        return PAGE_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        // Метод провайдера отрабатывает также на этапе Transform
        // (@see AbstractActionTransformer.mapSecurity).
        // На этом этапе для {id} не установлено значение.
        if (context.contains("{") || context.contains("}"))
            return singletonList(new N2oSimplePage());

        SyncMappingRequest request = toRequest(context);
        return singletonList(createPage(context, request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oPage.class);
    }

    private N2oSimplePage createPage(String context, SyncMappingRequest request) {

        N2oSimplePage page = new N2oSimplePage();
        page.setId(PAGE_PROVIDER_ID + "?" + context);

        page.setWidget(createForm(context, request));

        return page;
    }

    private N2oForm createForm(String context, SyncMappingRequest request) {

        N2oForm n2oForm = new N2oForm();
        n2oForm.setQueryId(SyncMappingQueryProvider.QUERY_PROVIDER_ID + "?" + context);
        n2oForm.setObjectId(SyncMappingObjectProvider.OBJECT_PROVIDER_ID + "?" + context);

        //n2oForm.setPreFilters(createPreFilters());

        //n2oForm.setItems(createPageFields(request));

        return n2oForm;
    }
}
