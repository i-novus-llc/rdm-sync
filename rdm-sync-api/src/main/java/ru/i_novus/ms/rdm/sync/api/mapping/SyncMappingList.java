package ru.i_novus.ms.rdm.sync.api.mapping;

import ru.i_novus.ms.rdm.api.exception.RdmException;

import java.util.List;
import java.util.stream.Collectors;

public class SyncMappingList {

    public static void validate(List<SyncMapping> mappings){
        //проверяем на пересечение
        if (checkForOverlap(mappings.stream().map(m -> m.getVersionMapping().getRange()).collect(Collectors.toList()))) {
            throw new RdmException("Overlapping version ranges detected");
        }
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
