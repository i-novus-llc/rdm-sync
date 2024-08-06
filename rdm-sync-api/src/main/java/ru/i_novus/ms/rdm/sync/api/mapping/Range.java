package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.*;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Класс Range представляет диапазон версий и методы для сравнения версий.
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Range implements Comparable<Range>, Serializable {

    /**
     * Строковое представление диапазона версий.
     */
    private String range;

    private static final int MAX_PART = Integer.MAX_VALUE;
    private static final int MIN_PART = Integer.MIN_VALUE;
    private static final String MAX_VERSION = "999999999.999999999";
    private static final String MIN_VERSION = "-999999999.-999999999";

    /**
     * Проверяет, содержит ли диапазон указанную версию.
     *
     * @param version Версия для проверки
     * @return true, если версия содержится в диапазоне, иначе false
     */
    public boolean containsVersion(String version) {

        if (range == null) {
            return true;
        }

        if (range.contains(",")) {
            return Arrays.stream(range.split(",")).anyMatch(splitRange -> new Range(splitRange).containsVersion(version));
        }
        if (range.equals("*")) {
            return true;
        }

        if (range.contains("-")) {
            String[] parts = range.split("-");
            String start = parts[0].trim();
            String end = parts.length > 1 ? parts[1].trim() : "";

            if (start.equals("*")) {
                return compareVersions(version, end) <= 0;
            }

            if (end.equals("*") || end.isEmpty()) {
                return compareVersions(version, start) >= 0;
            }

            return compareVersions(version, start) >= 0 && compareVersions(version, end) <= 0;
        }

        return compareVersions(range, version) == 0;
    }

    @Override
    public int compareTo(Range other) {
        Range start = convertToEndVersion(new Range(range));
        Range end = convertToEndVersion(other);

        int[] parts1 = parseVersion(start.getRange());
        int[] parts2 = parseVersion(end.getRange());

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int part1 = i < parts1.length ? parts1[i] : 0;
            int part2 = i < parts2.length ? parts2[i] : 0;
            int comparison = Integer.compare(part1, part2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
    /**
     * Из диапазона версий берем его максимальное значение, с учетом знака "*"
     *
     * @param r Диапазон версий
     * @return Конвертированный диапазон версий
     */
    private Range convertToEndVersion(Range r){

        if (r.getRange() == null) {
            return new Range(MIN_VERSION);
        }

        String[] rangeParts = r.getRange().split("-");
        if (rangeParts.length > 1){
            if (rangeParts[1].equals("*")){
                rangeParts[1] = MAX_VERSION;
            }
            return new Range(rangeParts[1]);
        }
        return new Range(r.getRange());
    }

    /**
     * Разбирает строку версии на числовые компоненты.
     *
     * @param version Строковое представление версии
     * @return Массив числовых компонент версии
     */
    private int[] parseVersion(String version) {
        return Arrays.stream(version.split("\\."))
                .mapToInt(part -> {
                    if (part.equals("*")) {
                        return MAX_PART;
                    }
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException e) {
                        return 0; // значение по умолчанию
                    }
                })
                .toArray();
    }

    /**
     * Сравнивает две версии.
     *
     * @param v1 Первая версия
     * @param v2 Вторая версия
     * @return Результат сравнения (-1 если v1 < v2, 0 если v1 == v2, 1 если v1 > v2)
     */
    private int compareVersions(String v1, String v2) {
        int[] parts1 = parseVersion(v1);
        int[] parts2 = parseVersion(v2);

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int part1 = i < parts1.length ? parts1[i] : 0;
            int part2 = i < parts2.length ? parts2[i] : 0;
            int comparison = Integer.compare(part1, part2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    public boolean overlapsWith(Range other) {
        if (this.range == null || other.range == null) {
            return false;
        }

        if (this.range.equals("*") || other.range.equals("*")) {
            return true;
        }

        String[] thisParts = this.range.split("-");
        String[] otherParts = other.range.split("-");

        String thisStart = thisParts[0].trim();
        String thisEnd = thisParts.length > 1 ? thisParts[1].trim() : thisStart;

        String otherStart = otherParts[0].trim();
        String otherEnd = otherParts.length > 1 ? otherParts[1].trim() : otherStart;

        return (compareVersions(thisStart, otherEnd) <= 0 || thisStart.equals("*")) && (compareVersions(otherStart, thisEnd) <= 0 || thisEnd.equals("*"));
    }
}
