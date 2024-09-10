package ru.i_novus.ms.rdm.sync.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.impl.util.RdmMapper;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.sync.impl.util.RdmMapper.*;

@Slf4j
public class RdmSyncSourceService implements SyncSourceService {

    private static final String SEVERAL_REFBOOKS_WITH_CODE_FOUND =
            "Several reference books with code '%s' found.";

    private final RestClient restClient;

    public RdmSyncSourceService(String url) {
        this.restClient = RestClient.create(url);
    }

    @Override
    public RefBookVersion getRefBook(String code, String version) {

        final Map<String, Object> map;
        if (version != null) {
            map = getRefBookByVersion(code, version);
            if (CollectionUtils.isEmpty(map)) {
                log.warn("Cannot find version {} of refBook [{}]", version, code);
            }
        } else {
            map = getRefBookByCode(code);
            if (CollectionUtils.isEmpty(map)) {
                log.warn("Cannot find last published version of refBook [{}]", code);
            }
        }

        return RdmMapper.toRefBookVersion(map);
    }

    private Map<String, Object> getRefBookByVersion(String code, String version) {

        final String uri = String.format("/version/%s/refbook/%s", version, code);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error(String.format("Error get version %s of refBook [%s]", version, code), e);
            throw new RuntimeException("Failed to get refBook", e);
        }
    }

    private Map<String, Object> getRefBookByCode(String code) {

        final String uri = String.format("/version/refBook/%s/last", code);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error(String.format("Error get last published version of refBook [%s]", code), e);
            throw new RuntimeException("Failed to get refBook", e);
        }
    }

    @Override
    public List<RefBookVersionItem> getVersions(String code) {

        final Map<String, Object> map = getRefBookVersionsByCode(code);
        final List<RefBookVersionItem> result = map != null
                ? RdmMapper.toRefBookVersionItems(map.get("content"))
                : emptyList();
        if (CollectionUtils.isEmpty(result)) {
            log.warn("Cannot find versions of refBook [{}]", code);
        }

        return result;
    }

    private Map<String, Object> getRefBookVersionsByCode(String code) {

        final String uri = String.format("/version/versions?refBookCode=%s", code);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error(String.format("Error get versions of refBook [%s]", code), e);
            throw new RuntimeException("Failed to get versions", e);
        }
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {

        final String code = dataCriteria.getCode();

        if (dataCriteria.getVersionId() == null) {
            final String version = dataCriteria.getVersion();
            final Integer versionId = getRefBookVersionId(code, version);
            if (versionId == null) {
                throw new RuntimeException(String.format("Failed to get id for version %s of refBook [%s]", version, code));
            }
            dataCriteria.setVersionId(versionId);
        }

        final Map<String, Object> map = getRefBookVersionData(dataCriteria);
        if (CollectionUtils.isEmpty(map)) {
            log.warn("Cannot find data of version id={} of refBook [{}]", dataCriteria.getVersionId(), code);
            return new PageImpl<>(emptyList(), dataCriteria, 0);
        }

        final List<Map<String, Object>> result = toRefBookVersionData(map.get("content"), dataCriteria);
        final Long total = parseLong(map.get("totalElements"));
        return new PageImpl<>(result, dataCriteria, total != null ? total : result.size());
    }

    private Map<String, Object> getRefBookVersionData(DataCriteria criteria) {

        final String uri = String.format(
                "/version/%d/data?page=%d&size=%d",
                criteria.getVersionId(),
                criteria.getPageNumber(),
                criteria.getPageSize()
        );
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            final String errorMessage = String.format(
                    "Error get data of version id=%d of rebook [%s]: ",
                    criteria.getVersionId(), criteria.getCode()
            );
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to get data", e);
        }
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {

        final String code = criteria.getRefBookCode();
        final Integer oldVersionId = getRefBookVersionId(code, criteria.getOldVersion());
        final Integer newVersionId = getRefBookVersionId(code, criteria.getNewVersion());

        final Map<String, Object> structureDiff = compareRefBookVersionStructures(oldVersionId, newVersionId);
        if (isStructureChanged(structureDiff)) {
            return VersionsDiff.structureChangedInstance();
        }

        final Page<RowDiff> page;
        final Map<String, Object> dataDiff = compareRefBookVersionData(oldVersionId, newVersionId, criteria);
        if (CollectionUtils.isEmpty(dataDiff)) {
            log.warn("Cannot find data diff between versions: old id={}, new id={}", oldVersionId, newVersionId);
            page = new PageImpl<>(emptyList(), criteria, 0);

        } else {

            final Map<String, Object> map = toDataDiffRows(dataDiff);
            final List<RowDiff> result = toRefBookVersionRowDiff(map.get("content"), criteria);
            final Long total = parseLong(map.get("totalElements"));
            page = new PageImpl<>(result, criteria, total != null ? total : result.size());
        }

        return VersionsDiff.dataChangedInstance(page);
    }

    private Map<String, Object> compareRefBookVersionStructures(Integer oldVersionId,
                                                                Integer newVersionId) {

        final String uri = String.format("/compare/structures/%d-%d", oldVersionId, newVersionId);
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            final String errorMessage = String.format(
                    "Error get structure compare between versions: old id=%d, new id=%d",
                    oldVersionId, newVersionId
            );
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to get structure compare", e);
        }
    }

    private Map<String, Object> compareRefBookVersionData(Integer oldVersionId,
                                                          Integer newVersionId,
                                                          VersionsDiffCriteria criteria) {

        final String uri = String.format(
                "/compare/data?oldVersionId=%d&newVersionId=%d&page=%d&size=%d",
                oldVersionId,
                newVersionId,
                criteria.getPageNumber(),
                criteria.getPageSize()
        );
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            final String errorMessage = String.format(
                    "Error get data compare between versions: old id=%d, new id=%d",
                    oldVersionId, newVersionId
            );
            log.error(errorMessage, e);
            throw new RuntimeException("Failed to get data compare", e);
        }
    }

    private Integer getRefBookVersionId(String code, String version) {

        final Map<String, Object> map = getRefBookByVersion(code, version);
        if (CollectionUtils.isEmpty(map)) {
            log.warn("Cannot find id for version {} of refBook [{}]", version, code);
        }

        return (Integer) map.get(VERSION_ID_FIELD);
    }

}
