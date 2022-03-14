package ru.i_novus.ms.rdm.sync.api.service;

import brave.internal.Nullable;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.api.model.*;

import java.util.List;
import java.util.Map;

public interface SyncSourceService {

    RefBookVersion getRefBook(String code, @Nullable String version);

    List<RefBookVersionItem> getVersions(String code);

    Page<Map<String, Object>> getData(DataCriteria dataCriteria);

    VersionsDiff getDiff(VersionsDiffCriteria criteria);

}
