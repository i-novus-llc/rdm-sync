package ru.i_novus.ms.rdm.sync.api.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RangeTest {

    @Test
    void containsVersion_exactMatch() {
        Range range = new Range("1.0");
        assertTrue(range.containsVersion("1.0"));
        assertFalse(range.containsVersion("2.0"));
    }

    @Test
    void containsVersion_wildcard() {
        Range range = new Range("*");
        assertTrue(range.containsVersion("1.0"));
        assertTrue(range.containsVersion("2.0"));
    }

    @Test
    void containsVersion_startToEnd() {
        Range range = new Range("1.0-*");
        assertTrue(range.containsVersion("1.0"));
        assertTrue(range.containsVersion("2.0"));
    }

    @Test
    void containsVersion_withinRange() {
        Range range = new Range("1.0-2.0");
        assertTrue(range.containsVersion("1.0"));
        assertTrue(range.containsVersion("1.5"));
        assertTrue(range.containsVersion("2.0"));
        assertFalse(range.containsVersion("2.5"));
    }

    @Test
    void containsVersion_endWildcard() {
        Range range = new Range("*-2.0");
        assertTrue(range.containsVersion("1.0"));
        assertTrue(range.containsVersion("2.0"));
        assertFalse(range.containsVersion("2.5"));
    }
}