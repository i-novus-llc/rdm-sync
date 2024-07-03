package ru.i_novus.ms.rdm.sync.api.mapping;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Range implements Comparable<Range> {

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

    @Override
    public int compareTo(Range other) {
        // Сравнение для случаев "*"
        if (this.range.equals("*") && other.range.equals("*")) {
            return 0;
        }
        if (this.range.equals("*")) {
            return 1;
        }
        if (other.range.equals("*")) {
            return -1;
        }

        // Сравнение для диапазонов
        if (this.range.contains("-") && other.range.contains("-")) {
            String[] thisParts = this.range.split("-");
            String thisStart = thisParts[0].trim();
            String thisEnd = thisParts[1].trim();

            String[] otherParts = other.range.split("-");
            String otherStart = otherParts[0].trim();
            String otherEnd = otherParts[1].trim();

            int startComparison = thisStart.compareTo(otherStart);
            if (startComparison != 0) {
                return startComparison;
            }

            return thisEnd.compareTo(otherEnd);
        }

        // Сравнение для одиночных версий
        return this.range.compareTo(other.range);
    }
}
