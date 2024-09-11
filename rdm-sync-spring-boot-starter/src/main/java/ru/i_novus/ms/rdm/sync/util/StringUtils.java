package ru.i_novus.ms.rdm.sync.util;

public final class StringUtils {

    public static final String ESCAPE_CHAR = "\\";
    public static final String DOUBLE_QUOTE_CHAR = "\"";
    public static final String SINGLE_QUOTE_CHAR = "'";

    private StringUtils() {
        // Nothing to do.
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String addDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value + DOUBLE_QUOTE_CHAR;
    }

    public static String addSingleQuotes(String value) {
        return SINGLE_QUOTE_CHAR + value + SINGLE_QUOTE_CHAR;
    }

    public static String toDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value.replace(DOUBLE_QUOTE_CHAR, ESCAPE_CHAR + DOUBLE_QUOTE_CHAR) + DOUBLE_QUOTE_CHAR;
    }
}
