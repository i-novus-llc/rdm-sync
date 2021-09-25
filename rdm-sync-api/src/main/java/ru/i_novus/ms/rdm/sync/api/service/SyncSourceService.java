package ru.i_novus.ms.rdm.sync.api.service;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.api.model.DataCriteria;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.VersionsDiff;
import ru.i_novus.ms.rdm.sync.api.model.VersionsDiffCriteria;

import java.util.Map;

public interface SyncSourceService {

    RefBook getRefBook(String code);

    Page<Map<String, Object>> getData(DataCriteria dataCriteria);

    VersionsDiff getDiff(VersionsDiffCriteria criteria);

}
