package ru.i_novus.ms.rdm.sync.api.service;

import brave.internal.Nullable;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.api.model.*;

import java.util.Map;

public interface SyncSourceService {

    RefBookVersion getRefBook(String code, @Nullable String version);

    Page<Map<String, Object>> getData(DataCriteria dataCriteria);

    VersionsDiff getDiff(VersionsDiffCriteria criteria);

}
