package com.liferay.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MapUtilTest {

    /**
     * Method to test: {@link MapUtil#invertMap(Map)}
     * When: map contains unique values for all keys
     * Should: return a new map with keys and values swapped for every entry
     */
    @Test
    public void testInvertMap_WhenUniqueValues_ShouldSwapKeysAndValues() {
        Map<String, String> input = new HashMap<>();
        input.put("a", "1");
        input.put("b", "2");
        input.put("c", "3");

        Map<String, String> inverted = MapUtil.invertMap(input);

        assertEquals(3, inverted.size());
        assertEquals("a", inverted.get("1"));
        assertEquals("b", inverted.get("2"));
        assertEquals("c", inverted.get("3"));
    }

    /**
     * Method to test: {@link MapUtil#invertMap(Map)}
     * When: map is empty
     * Should: return an empty map
     */
    @Test
    public void testInvertMap_WhenEmptyMap_ShouldReturnEmptyMap() {
        Map<String, String> input = new HashMap<>();
        Map<String, String> inverted = MapUtil.invertMap(input);
        assertNotNull(inverted);
        assertTrue(inverted.isEmpty());
    }

    /**
     * Method to test: {@link MapUtil#invertMap(Map)}
     * When: map contains a single entry
     * Should: return a map with that single entry inverted
     */
    @Test
    public void testInvertMap_WhenSingleEntry_ShouldReturnSingleInverted() {
        Map<String, String> input = new HashMap<>();
        input.put("key", "value");
        Map<String, String> inverted = MapUtil.invertMap(input);
        assertEquals(1, inverted.size());
        assertEquals("key", inverted.get("value"));
    }

    /**
     * Method to test: {@link MapUtil#invertMap(Map)}
     * When: map contains duplicate values for different keys
     * Should: throw IllegalStateException due to key collision in the inverted map
     */
    @Test(expected = IllegalStateException.class)
    public void testInvertMap_WhenDuplicateValues_ShouldThrowIllegalStateException() {
        Map<String, String> input = new HashMap<>();
        input.put("k1", "dup");
        input.put("k2", "dup"); // duplicate value becomes duplicate key
        MapUtil.invertMap(input);
    }


    /**
     * Method to test: {@link MapUtil#invertMap(Map)}
     * When: inverting a map
     * Should: not modify the original map and return a different instance with swapped entries
     */
    @Test
    public void testInvertMap_ShouldNotModifyOriginalMap() {
        Map<String, String> input = new HashMap<>();
        input.put("x", "10");
        input.put("y", "20");

        Map<String, String> copyBefore = new HashMap<>(input);
        Map<String, String> inverted = MapUtil.invertMap(input);

        // Ensure original map remains the same
        assertEquals(copyBefore, input);
        // Ensure inverted is a different instance and not the same reference
        assertNotSame(input, inverted);
    }
}
