package com.dotcms.util;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LowerKeyMapTest {

    @Test
    public void put_lowerKeyMapTest()  {

        final Map<String, Object> lowerMap =
                new LowerKeyMap<>();

        lowerMap.put("HELLO", "HELLO");
        lowerMap.put("heLLO", "HI");
        lowerMap.put("heLLo", "HI ALL");
        lowerMap.put("AAAAAAAA", "A");
        lowerMap.put("aaaaaaaa", "a");
        lowerMap.put("AAAaaaaa", "aA");

        assertEquals(2, lowerMap.size());
        assertEquals("aA", lowerMap.get("AAAaaaaa"));
        assertEquals("HI ALL", lowerMap.get("heLLo"));
    }
}
