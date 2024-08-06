package ru.i_novus.ms.rdm.sync.api.mapping;

import ru.i_novus.ms.rdm.sync.api.exception.MappingOverlapsException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MappingOverlapsValidator {

    private MappingOverlapsValidator() {}

    public static void validate(List<SyncMapping> mappings){
        Map<String, List<SyncMapping>> groupedByRefBookCode = mappings.stream()
                .collect(Collectors.groupingBy(m -> m.getVersionMapping().getCode()));

        groupedByRefBookCode.forEach((refBookCode, mappingGroup) -> {
            List<Range> ranges = mappingGroup.stream()
                    .map(m -> m.getVersionMapping().getRange())
                    .collect(Collectors.toList());

            checkForOverlap(ranges, refBookCode);
        });
    }

    private static void checkForOverlap(List<Range> ranges, String refBookCode) {
        for(Range range1 : ranges){
            Optional<Range> overlappingRange = ranges.stream()
                    .filter(range2 -> !range2.equals(range1) && range2.overlapsWith(range1))
                    .findAny();
            if(overlappingRange.isPresent()) {
                throw new MappingOverlapsException(range1.getRange(), overlappingRange.get().getRange(), refBookCode);
            }

        }
    }
}
