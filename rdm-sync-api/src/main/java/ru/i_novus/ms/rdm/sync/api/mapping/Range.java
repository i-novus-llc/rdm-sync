package ru.i_novus.ms.rdm.sync.api.mapping;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Range {

    private String range;

    boolean containsVersion(String version) {
        if (range.equals("*")) {
            return true;
        }

        if (range.contains("-")) {
            String[] parts = range.split("-");
            String start = parts[0].trim();
            String end = parts[1].trim();

            if (start.equals("*")) {
                return version.compareTo(end) <= 0;
            }

            if (end.equals("*")) {
                return version.compareTo(start) >= 0;
            }

            return version.compareTo(start) >= 0 && version.compareTo(end) <= 0;
        }

        return range.equals(version);
    }
}
