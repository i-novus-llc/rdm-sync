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

    private final RestClient restClient;

    public RdmSyncSourceService(RestClient restClient) {
        this.restClient = restClient;
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

        final String uri = "/version/{version}/refbook/{code}";
        try {
            return restClient.get()
                    .uri(uri, version, code)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error get version {} of refBook [{}]", version, code, e);
            throw new RuntimeException("Failed to get refBook", e);
        }
    }

    private Map<String, Object> getRefBookByCode(String code) {

        final String uri = "/version/refBook/{code}/last";
        try {
            return restClient.get()
                    .uri(uri, code)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error get last published version of refBook [{}]", code, e);
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

        final String uri = "/version/versions?refBookCode={code}";
        try {
            return restClient.get()
                    .uri(uri, code)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error get versions of refBook [{}]", code, e);
            throw new RuntimeException("Failed to get versions", e);
        }
    }

    @Override
    public Page<Map<String, Object>> getData(DataCriteria dataCriteria) {

        final String code = dataCriteria.getCode();

        final Integer versionId = getAbsentVersionId(dataCriteria.getVersion(), code, dataCriteria.getVersionId());
        dataCriteria.setAbsentVersionId(versionId);

        final Map<String, Object> map = getRefBookVersionData(dataCriteria);
        if (CollectionUtils.isEmpty(map)) {
            log.warn("Cannot find data of version id={} of refBook [{}]", versionId, code);
            return new PageImpl<>(emptyList(), dataCriteria, 0);
        }

        final List<Map<String, Object>> result = toRefBookVersionData(map.get("content"), dataCriteria);
        final Long total = parseLong(map.get("totalElements"));
        return new PageImpl<>(result, dataCriteria, total != null ? total : result.size());
    }

    private Map<String, Object> getRefBookVersionData(DataCriteria criteria) {

        final String uri = "/version/{versionId}/data?page={page}&size={size}";
        try {
            return restClient.get()
                    .uri(uri,
                            criteria.getVersionId(),
                            criteria.getPageNumber(),
                            criteria.getPageSize())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error get data of version id={} of rebook [{}]: ",
                    criteria.getVersionId(), criteria.getCode(), e);
            throw new RuntimeException("Failed to get data", e);
        }
    }

    @Override
    public VersionsDiff getDiff(VersionsDiffCriteria criteria) {

        final String code = criteria.getRefBookCode();

        final Integer oldVersionId = getAbsentVersionId(code, criteria.getOldVersion(), criteria.getOldVersionId());
        criteria.setAbsentOldVersionId(oldVersionId);

        final Integer newVersionId = getAbsentVersionId(code, criteria.getNewVersion(), criteria.getNewVersionId());
        criteria.setAbsentNewVersionId(newVersionId);

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

        final String uri = "/compare/structures/{oldVersionId}-{newVersionId}";
        try {
            return restClient.get()
                    .uri(uri, oldVersionId, newVersionId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error get structure compare for versions: old id={}, new id={}", oldVersionId, newVersionId, e);
            throw new RuntimeException("Failed to get structure compare", e);
        }
    }

    private Map<String, Object> compareRefBookVersionData(Integer oldVersionId,
                                                          Integer newVersionId,
                                                          VersionsDiffCriteria criteria) {

        final String uri = "/compare/data?oldVersionId={oldVersionId}&newVersionId={newVersionId}" +
                "&page={page}&size={size}";
        try {
            return restClient.get()
                    .uri(uri,
                            oldVersionId,
                            newVersionId,
                            criteria.getPageNumber(),
                            criteria.getPageSize())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception e) {
            log.error("Error get data compare for versions: old id={}, new id={}", oldVersionId, newVersionId, e);
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

    private Integer getAbsentVersionId(String version, String code, Integer versionId) {

        if (versionId != null)
            return versionId;

        final Integer result = getRefBookVersionId(code, version);
        if (result == null) {
            throw new RuntimeException(String.format("Failed to get id for version %s of refBook [%s]", version, code));
        }
        return result;
    }

}
