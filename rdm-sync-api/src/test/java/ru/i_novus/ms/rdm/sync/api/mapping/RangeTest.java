package ru.i_novus.ms.rdm.sync.api.mapping;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Test
    void containsVersion_endWildcardLongNum() {
        Range range = new Range("*-2.13");
        assertTrue(range.containsVersion("1.0"));
        assertTrue(range.containsVersion("2.0"));
        assertFalse(range.containsVersion("2.15"));
    }


    @Test
    void testRangeSorting() {
        List<Range> ranges = new ArrayList<>();
        ranges.add(new Range("*"));
        ranges.add(new Range("2.14-2.15"));
        ranges.add(new Range("2.2-2.3"));
        ranges.add(new Range("3.1-*"));
        ranges.add(new Range("0.5-1.5"));
        ranges.add(new Range("*-0.2"));
        ranges.add(new Range("0.6"));
        ranges.add(new Range("1.0-2.0"));
        ranges.add(new Range("2.13"));
        ranges.add(new Range("1.2-1.8"));

        Collections.sort(ranges);

        assertEquals("*-0.2", ranges.get(0).getRange());
        assertEquals("0.6", ranges.get(1).getRange());
        assertEquals("0.5-1.5", ranges.get(2).getRange());
        assertEquals("1.2-1.8", ranges.get(3).getRange());
        assertEquals("1.0-2.0", ranges.get(4).getRange());
        assertEquals("2.2-2.3", ranges.get(5).getRange());
        assertEquals("2.13", ranges.get(6).getRange());
        assertEquals("2.14-2.15", ranges.get(7).getRange());
        assertEquals("3.1-*", ranges.get(8).getRange());
        assertEquals("*", ranges.get(9).getRange());

    }

    /**
     * проверка принадлежности версии когда диапазон задан через запятую
     */
    @Test
    void testContainsVersionInMultipleRange() {
        Range range  = new Range("1.0-2.9,5.0-*");
        assertTrue(range.containsVersion("1.1"));
        assertTrue(range.containsVersion("2"));
        assertTrue(range.containsVersion("5.0"));
        assertTrue(range.containsVersion("7"));
        assertFalse(range.containsVersion("3"));

    }

    /**
     * проверка пересечения диапазонов когда диапазоны заданы через запятую
     */
    @Test
    void testOverlapsRangesInMultipleRange() {
        Range range1  = new Range("1.0-2.9,5.0-*");
        Range range2 = new Range("3.0-3.9,6.0-*");
        assertTrue(range1.overlapsWith(range2));
    }

}