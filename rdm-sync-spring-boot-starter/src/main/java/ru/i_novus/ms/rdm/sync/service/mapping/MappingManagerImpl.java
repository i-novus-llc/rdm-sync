package ru.i_novus.ms.rdm.sync.service.mapping;

import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.SyncMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MappingManagerImpl implements MappingManager {

    private final RdmSyncDao dao;

    public MappingManagerImpl(RdmSyncDao dao) {
        this.dao = dao;
    }

    @Override
    public List<SyncMapping> validateAndGetMappingsToUpdate(List<SyncMapping> mappings) {
        if (checkForOverlap(mappings.stream().map(m -> m.getVersionMapping().getRange()).collect(Collectors.toList()))) {
            throw new RdmException("Overlapping version ranges detected");
        }

        List<VersionMapping> existingMappings = dao.getVersionMappings();

        // Определяем маппинги, которые нужно удалить
        existingMappings.stream()
                .filter(existing -> mappings.stream().noneMatch(m -> m.getVersionMapping().equals(existing)))
                .forEach(dao::markIsDeletedVersionMapping);

        // Определяем маппинги, которые нужно добавить или обновить
        return mappings.stream()
                .flatMap(newMapping -> {
                    VersionMapping existingMapping = existingMappings.stream()
                            .filter(m -> m.equals(newMapping.getVersionMapping()))
                            .findFirst()
                            .orElse(null);

                    if (existingMapping != null) {
                        if (newMapping.getMappingVersion() == existingMapping.getMappingVersion() + 1 ||
                                !newMapping.getVersionMapping().getRange().equals(existingMapping.getRange())) {
                            return Stream.of(newMapping);
                        } else {
                            return Stream.empty();
                        }
                    } else {
                        return Stream.of(newMapping);
                    }
                })
                .collect(Collectors.toList());
    }

    public static boolean checkForOverlap(List<Range> ranges) {
        return ranges.stream()
                .flatMap(range1 -> ranges.stream()
                        .filter(range2 -> !range1.equals(range2))
                        .filter(range1::overlapsWith))
                .findAny()
                .isPresent();
    }


}
