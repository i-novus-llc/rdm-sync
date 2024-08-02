package ru.i_novus.ms.rdm.sync.api.mapping;

import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SyncMappingList {

    public static void validate(List<SyncMapping> mappings){
        Map<String, List<SyncMapping>> groupedByRefBookCode = mappings.stream()
                .collect(Collectors.groupingBy(m -> m.getVersionMapping().getCode()));

        groupedByRefBookCode.forEach((refBookCode, mappingGroup) -> {
            List<Range> ranges = mappingGroup.stream()
                    .map(m -> m.getVersionMapping().getRange())
                    .collect(Collectors.toList());

            if (checkForOverlap(ranges)) {
                throw new RdmSyncException("Overlapping version ranges detected for refBookCode: " + refBookCode);
            }
        });
    }

    private static boolean checkForOverlap(List<Range> ranges) {
        return ranges.stream()
                .flatMap(range1 -> ranges.stream()
                        .filter(range2 -> !range1.equals(range2))
                        .filter(range1::overlapsWith))
                .findAny()
                .isPresent();
    }
}
