package ru.i_novus.ms.rdm.sync.api.mapping;

import lombok.*;

import java.util.Arrays;

/**
 * Класс Range представляет диапазон версий и методы для сравнения версий.
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Range implements Comparable<Range> {

    /**
     * Строковое представление диапазона версий.
     */
    private String range;

    private static final int MAX_PART = Integer.MAX_VALUE;
    private static final int MIN_PART = Integer.MIN_VALUE;

    /**
     * Проверяет, содержит ли диапазон указанную версию.
     *
     * @param version Версия для проверки
     * @return true, если версия содержится в диапазоне, иначе false
     */
    public boolean containsVersion(String version) {
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
        if (this.range.equals("*") && other.range.equals("*")) {
            return 0;
        }
        if (this.range.equals("*")) {
            return 1;
        }
        if (other.range.equals("*")) {
            return -1;
        }

        if (this.range.contains("-") && other.range.contains("-")) {
            return compareRanges(this.range, other.range, true);
        }

        if (this.range.contains("-")) {
            String[] thisParts = this.range.split("-");
            String thisStart = thisParts[0].trim();
            thisStart = handleWildcard(thisStart, true);

            return compareVersions(thisStart, other.range);
        }

        if (other.range.contains("-")) {
            String[] otherParts = other.range.split("-");
            String otherStart = otherParts[0].trim();
            otherStart = handleWildcard(otherStart, true);

            return compareVersions(this.range, otherStart);
        }

        return compareVersions(this.range, other.range);
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

    /**
     * Обрабатывает символ * в версии.
     *
     * @param versionPart Часть версии
     * @param isStart     Флаг, указывающий на начальную или конечную часть диапазона
     * @return Преобразованная часть версии
     */
    private String handleWildcard(String versionPart, boolean isStart) {
        if (versionPart.equals("*")) {
            return isStart ? String.valueOf(MIN_PART) : String.valueOf(MAX_PART);
        }
        return versionPart;
    }

    /**
     * Сравнивает два диапазона версий.
     *
     * @param thisRange  Первый диапазон
     * @param otherRange Второй диапазон
     * @param isStart    Флаг, указывающий на начальную или конечную часть диапазона
     * @return Результат сравнения (-1 если thisRange < otherRange, 0 если thisRange == otherRange, 1 если thisRange > otherRange)
     */
    private int compareRanges(String thisRange, String otherRange, boolean isStart) {
        String[] thisParts = thisRange.split("-");
        String thisStart = thisParts[0].trim();
        String thisEnd = thisParts.length > 1 ? thisParts[1].trim() : "";

        String[] otherParts = otherRange.split("-");
        String otherStart = otherParts[0].trim();
        String otherEnd = otherParts.length > 1 ? otherParts[1].trim() : "";

        thisStart = handleWildcard(thisStart, isStart);
        thisEnd = handleWildcard(thisEnd, !isStart);
        otherStart = handleWildcard(otherStart, isStart);
        otherEnd = handleWildcard(otherEnd, !isStart);

        int startComparison = compareVersions(thisStart, otherStart);
        if (startComparison != 0) {
            return startComparison;
        }

        return compareVersions(thisEnd, otherEnd);
    }
}
