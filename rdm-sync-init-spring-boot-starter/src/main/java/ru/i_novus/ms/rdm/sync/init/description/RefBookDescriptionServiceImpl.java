package ru.i_novus.ms.rdm.sync.init.description;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.init.dao.SyncSourceDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@Transactional(propagation = Propagation.NEVER)
public class RefBookDescriptionServiceImpl implements RefBookDescriptionService {

    private final SyncSourceDao syncSourceDao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    private final EnrichCommentsMode enrichCommentsMode;
    private final Integer enrichCommentsSearchDepth;

    public RefBookDescriptionServiceImpl(
            SyncSourceDao syncSourceDao,
            Set<SyncSourceServiceFactory> syncSourceServiceFactories,
            @Value("${rdm-sync.auto-create.enrich-comments.mode:NEVER}") EnrichCommentsMode enrichCommentsMode,
            @Value("${rdm-sync.auto-create.enrich-comments.search-depth:3}") Integer enrichCommentsSearchDepth
    ) {
        this.syncSourceDao = syncSourceDao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
        this.enrichCommentsMode = enrichCommentsMode;
        this.enrichCommentsSearchDepth = enrichCommentsSearchDepth;
    }

    @Override
    public RefBookDescription getRefBookDescription(SyncMapping mapping) {
        String table = mapping.getVersionMapping().getTable();
        log.debug("Get descriptions for {}. {}", table, mapping);

        String refBookName = mapping.getVersionMapping().getRefBookName();
        String refBookDescription = isNotBlank(refBookName) ? mapping.getVersionMapping().getRefBookName() : null;

        List<FieldMapping> fieldMappings = mapping.getFieldMapping();
        Map<String, String> fieldDescriptions = fieldMappings.stream()
                .filter(fieldMapping -> isNotBlank(fieldMapping.getComment()))
                .collect(Collectors.toMap(FieldMapping::getSysField, FieldMapping::getComment));

        RefBookDescription result = switch (enrichCommentsMode) {
            case NEVER -> new RefBookDescription(refBookDescription, fieldDescriptions);
            case ALWAYS -> getOrFetchDescriptions(mapping, refBookDescription, fieldDescriptions);
            case ON_CREATE -> syncSourceDao.tableExists(mapping.getVersionMapping().getTable()) ?
                    new RefBookDescription(refBookDescription, fieldDescriptions)
                    : getOrFetchDescriptions(mapping, refBookDescription, fieldDescriptions);
        };
        log.debug("Result refBook description: {}", result);
        return result;
    }

    private RefBookDescription getOrFetchDescriptions(
            SyncMapping mapping,
            String refBookDescription,
            Map<String, String> fieldDescriptions
    ) {
        String table = mapping.getVersionMapping().getTable();
        String source = mapping.getVersionMapping().getSource();
        Map<String, List<String>> rdmFieldToSysFieldMapping = extractRdmFieldToSysFieldMapping(mapping);

        if (allDescriptionsDefined(refBookDescription, fieldDescriptions, rdmFieldToSysFieldMapping)) {
            log.debug("Mapping contains all descriptions, skip fetching from {}", source);
            return new RefBookDescription(refBookDescription, fieldDescriptions);
        } else {
            log.info("Fetch absent descriptions for {} from {}", table, source);
            return fetchUndefinedDescriptions(mapping, refBookDescription, fieldDescriptions, rdmFieldToSysFieldMapping);
        }
    }

    private static Map<String, List<String>> extractRdmFieldToSysFieldMapping(SyncMapping mapping) {
        Map<String, List<String>> rdmFieldToSysFieldMapping = new HashMap<>();
        for (FieldMapping fieldMapping : mapping.getFieldMapping()) {
            if (rdmFieldToSysFieldMapping.containsKey(fieldMapping.getRdmField())) {
                rdmFieldToSysFieldMapping.get(fieldMapping.getRdmField()).add(fieldMapping.getSysField());
            } else {
                rdmFieldToSysFieldMapping.put(fieldMapping.getRdmField(), new ArrayList<>(List.of(fieldMapping.getSysField())));
            }
        }
        return rdmFieldToSysFieldMapping;
    }

    private RefBookDescription fetchUndefinedDescriptions(
            SyncMapping mapping,
            String refBookDescription,
            Map<String, String> fieldDescriptions,
            Map<String, List<String>> rdmFieldToSysFieldMapping
    ) {
        SyncSourceService syncSourceService = getSyncSourceService(mapping.getVersionMapping().getSource());
        List<RefBookVersionItem> versions = new ArrayList<>(
                syncSourceService.getVersions(mapping.getVersionMapping().getCode())
        );
        versions.sort(Comparator.comparing(RefBookVersionItem::getFrom));

        int processableVersionIndex = versions.size() - 1;
        while (processableVersionIndex > -1
                && !allDescriptionsDefined(refBookDescription, fieldDescriptions, rdmFieldToSysFieldMapping)
                && !searchDepthReached(processableVersionIndex, versions.size())
        ) {
            RefBookVersionItem version = versions.get(processableVersionIndex);
            RefBookVersion refBook = syncSourceService.getRefBook(version.getCode(), version.getVersion());
            if (isBlank(refBookDescription)) {
                refBookDescription = refBook.getStructure().getRefDescription();
            }
            boolean allFieldDescriptionsDefined = fieldDescriptions.size() == rdmFieldToSysFieldMapping.size();
            if (!allFieldDescriptionsDefined) {
                Set<RefBookStructure.Attribute> shouldExtractDescriptionAttributes = refBook.getStructure()
                        .getAttributes().stream()
                        .filter(attr -> shouldExtractDescription(attr, fieldDescriptions, rdmFieldToSysFieldMapping))
                        .collect(Collectors.toSet());
                shouldExtractDescriptionAttributes.forEach(attr -> {
                    List<String> sysFields = rdmFieldToSysFieldMapping.get(attr.code());
                    sysFields.stream()
                            .filter(sysField -> !fieldDescriptions.containsKey(sysField))
                            .forEach(sysField -> fieldDescriptions.put(sysField, attr.description()));
                });
            }
            processableVersionIndex--;
        }
        return new RefBookDescription(refBookDescription, fieldDescriptions);
    }

    private boolean searchDepthReached(int processableVersionIndex, int versionsTotalSize) {
        return enrichCommentsSearchDepth <= (versionsTotalSize - 1 - processableVersionIndex);
    }

    private static boolean shouldExtractDescription(
            RefBookStructure.Attribute attr,
            Map<String, String> fieldDescriptions,
            Map<String, List<String>> rdmFieldToSysFieldMapping
    ) {
        boolean definitionExpected = rdmFieldToSysFieldMapping.containsKey(attr.code());
        boolean alreadyDefined = rdmFieldToSysFieldMapping.containsKey(attr.code())
                && rdmFieldToSysFieldMapping.get(attr.code()).stream().allMatch(fieldDescriptions::containsKey);
        return isNotBlank(attr.description()) && definitionExpected && !alreadyDefined;
    }

    private static boolean allDescriptionsDefined(
            String refBookDescription,
            Map<String, String> fieldDescriptions,
            Map<String, List<String>> rdmFieldToSysFieldMapping
    ) {
        boolean refBookDescriptionDefined = isNotBlank(refBookDescription);
        boolean allFieldDescriptionsDefined = fieldDescriptions.size() == rdmFieldToSysFieldMapping.size();
        return refBookDescriptionDefined && allFieldDescriptionsDefined;
    }

    private SyncSourceService getSyncSourceService(String sourceCode) {

        final SyncSource source = syncSourceDao.findByCode(sourceCode);
        return syncSourceServiceFactories.stream()
                .filter(factory -> factory.isSatisfied(source))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find factory by " + source.getFactoryName()))
                .createService(source);
    }
}
