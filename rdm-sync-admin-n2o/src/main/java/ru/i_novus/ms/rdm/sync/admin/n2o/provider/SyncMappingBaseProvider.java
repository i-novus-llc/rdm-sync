package ru.i_novus.ms.rdm.sync.admin.n2o.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.sync.admin.api.model.book.SyncRefBook;
import ru.i_novus.ms.rdm.sync.admin.api.model.book.SyncRefBookCriteria;
import ru.i_novus.ms.rdm.sync.admin.api.service.SyncAdminService;
import ru.i_novus.ms.rdm.sync.admin.n2o.model.SyncMappingRequest;

@Service
public class SyncMappingBaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(SyncMappingBaseProvider.class);

    private static final String CONTEXT_PARAM_SEPARATOR_REGEX = "&";

    protected SyncAdminService syncAdminService;

    @Autowired
    public void setSyncAdminService(SyncAdminService syncAdminService) {
        this.syncAdminService = syncAdminService;
    }

    /**
     * Получение запроса из контекста провайдера.
     *
     * @param context параметры провайдера в формате
     *                sourceCode&code&version&dataAction,
     *                где
     *                  sourceCode  - код (идентификатор) источника,
     *                  code        - код справочника,
     *                  version     - версия (номер),
     *                  actionType  - тип выполняемого действия.
     * @return Запрос
     */
    protected SyncMappingRequest toRequest(String context) {

        SyncMappingRequest request = new SyncMappingRequest();
        String[] params = context.split(CONTEXT_PARAM_SEPARATOR_REGEX);

        request.setSourceCode(params[0]);
        request.setCode(params[1]);
        request.setVersion(params[2]);
        request.setActionType(params[3]);

        request.setRefBook(findRefBook(request));

        return request;
    }

    /**
     * Поиск справочника по запросу
     *
     * @param request запрос
     * @return Справочник
     */
    protected SyncRefBook findRefBook(SyncMappingRequest request) {

        SyncRefBookCriteria criteria = new SyncRefBookCriteria();
        criteria.setSourceCode(request.getSourceCode());
        criteria.setCode(request.getCode());
        criteria.setVersion(request.getVersion());

        try {
            return syncAdminService.findSourceRefBook(criteria);

        } catch (Exception e) {
            logger.error("RefBook is not received for metadata", e);

            return null;
        }
    }
}
