package ru.i_novus.ms.rdm.sync.init.description;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSource;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersionItem;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceServiceFactory;
import ru.i_novus.ms.rdm.sync.api.dao.SyncSourceSavingDao;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(propagation = Propagation.NEVER)
public class RefBookDescriptionServiceImpl implements RefBookDescriptionService {

    private final SyncSourceSavingDao syncSourceDao;

    private final Set<SyncSourceServiceFactory> syncSourceServiceFactories;

    private final Boolean refreshComments;
    private final Integer refreshCommentsSearchDepth;

    public RefBookDescriptionServiceImpl(SyncSourceSavingDao syncSourceDao,
                                         Set<SyncSourceServiceFactory> syncSourceServiceFactories,
                                         @Value("${rdm-sync.auto-create.refresh-comments:true}") Boolean refreshComments,
                                         @Value("${rdm-sync.auto-create.refresh-comments.search-depth:3}") Integer refreshCommentsSearchDepth) {
        this.syncSourceDao = syncSourceDao;
        this.syncSourceServiceFactories = syncSourceServiceFactories;
        this.refreshComments = refreshComments;
        this.refreshCommentsSearchDepth = refreshCommentsSearchDepth;
    }

    @Override
    public RefBookDescription getRefBookDescription(SyncMapping mapping) {
        boolean tableExists = syncSourceDao.tableExists(mapping.getVersionMapping().getTable());
        if (tableExists && !refreshComments)
            return new RefBookDescription(null, Map.of());

        SyncSourceService syncSourceService = getSyncSourceService(mapping.getVersionMapping().getSource());
        List<RefBookVersionItem> versions = new ArrayList<>(
                syncSourceService
                        .getVersions(mapping.getVersionMapping().getCode())
        );
        versions.sort(Comparator.comparing(RefBookVersionItem::getFrom));
        Map<String, String> attributeDescriptions = mapping.getFieldMapping().stream()
                .filter(fieldMapping -> Objects.nonNull(fieldMapping.getComment()))
                .collect(Collectors.toMap(FieldMapping::getSysField, FieldMapping::getComment));

        int i = versions.size() -1;
        String refDescription = null;
        Map<String, String> rdmFieldToSysFieldMapping = mapping.getFieldMapping().stream()
                .collect(Collectors.toMap(FieldMapping::getRdmField, FieldMapping::getSysField));
        while (i > -1 && attributeDescriptions.size() < rdmFieldToSysFieldMapping.size()
                && refreshCommentsSearchDepth > (versions.size() - 1 - i)) {
            RefBookVersionItem version = versions.get(i);
            RefBookVersion refBook = syncSourceService.getRefBook(version.getCode(), version.getVersion());
            refBook.getStructure().getAttributes().forEach(attr -> {
                if(attr.description() != null && rdmFieldToSysFieldMapping.containsKey(attr.code())
                        && !attributeDescriptions.containsKey(rdmFieldToSysFieldMapping.get(attr.code()))) {
                    attributeDescriptions.put(rdmFieldToSysFieldMapping.get(attr.code()), attr.description());
                }
            });
            if (refDescription == null) {
                refDescription = refBook.getStructure().getRefDescription();
            }
            i--;
        }

        return new RefBookDescription(refDescription, attributeDescriptions);
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
