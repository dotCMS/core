package com.dotcms.util;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.CollectionsUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class CollectionsUtilsTest {


    /**
     * Testing the new Instance
     *
     */
    @Test
    public void collectionTest()  {

        final List<String> list1 = list();
        assertNotNull(list1);
        assertTrue(0 == list1.size());

        final List<Integer> list2 = list(1, 2, 3, 4, 5);
        assertNotNull(list2);
        assertTrue(5 == list2.size());
        assertTrue(1 == list2.get(0));
        assertTrue(2 == list2.get(1));
        assertTrue(3 == list2.get(2));
        assertTrue(4 == list2.get(3));
        assertTrue(5 == list2.get(4));

        final List<String> list3 = list("1", "2", "3", "4", "5");
        assertNotNull(list3);
        assertTrue(5 == list3.size());
        assertTrue("1".equals(list3.get(0)));
        assertTrue("2".equals(list3.get(1)));
        assertTrue("3".equals(list3.get(2)));
        assertTrue("4".equals(list3.get(3)));
        assertTrue("5".equals(list3.get(4)));

        final List<Float> list4 = list(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);
        assertNotNull(list4);
        assertTrue(5 == list4.size());
        assertTrue(1.1f == list4.get(0));
        assertTrue(2.2f == list4.get(1));
        assertTrue(3.3f == list4.get(2));
        assertTrue(4.4f == list4.get(3));
        assertTrue(5.5f == list4.get(4));

        final List<Long> list5 = list(1l, 2l, 3l, 4l, 5l);
        assertNotNull(list5);
        assertTrue(5 == list5.size());
        assertTrue(1l == list5.get(0));
        assertTrue(2l == list5.get(1));
        assertTrue(3l == list5.get(2));
        assertTrue(4l == list5.get(3));
        assertTrue(5l == list5.get(4));


        final List<Double> list6 = list(1.1d, 2.2d, 3.3d, 4.4d, 5.5d);
        assertNotNull(list6);
        assertTrue(5 == list6.size());
        assertTrue(1.1d == list6.get(0));
        assertTrue(2.2d == list6.get(1));
        assertTrue(3.3d == list6.get(2));
        assertTrue(4.4d == list6.get(3));
        assertTrue(5.5d == list6.get(4));

        final List<java.lang.Object> list7 = list("1",  new Double(2.2d), new Float(3.3f), new Integer(4), new Long(5l));
        assertNotNull(list7);
        assertTrue(5 == list7.size());
        assertTrue("1".equals(list7.get(0)));
        assertTrue(new Double(2.2d).equals(list7.get(1)));
        assertTrue(new Float(3.3f).equals(list7.get(2)));
        assertTrue(new Integer(4).equals(list7.get(3)));
        assertTrue(new Long(5l).equals(list7.get(4)));

        final Set<String> set1 = set();

        assertNotNull(set1);
        assertTrue(0 == set1.size());

        final Set<String> set2 = set("1","1","1","2","2","2","3","3","3");

        assertNotNull(set1);
        assertTrue(3 == set2.size());
        assertTrue(set2.contains("1"));
        assertTrue(set2.contains("2"));
        assertTrue(set2.contains("3"));

        final Set<Integer> set3 = set(1, 1, 1, 2, 2,  2,  3, 3 , 3);

        assertNotNull(set3);
        assertTrue(3 == set3.size());
        assertTrue(set3.contains(1));
        assertTrue(set3.contains(2));
        assertTrue(set3.contains(3));

        final Set<Float> set4 = set(1.1f, 1.2f, 1.1f, 2.3f, 2.3f,  2.2f,  3.1f, 3.1f , 3.2f);

        assertNotNull(set3);
        assertTrue(6 == set4.size());
        assertTrue(set4.contains(1.1f));
        assertTrue(set4.contains(1.2f));
        assertTrue(set4.contains(2.3f));
        assertTrue(set4.contains(2.2f));
        assertTrue(set4.contains(3.1f));
        assertTrue(set4.contains(3.2f));

        final Map<String, Integer> map1 = map();
        assertNotNull(map1);
        assertTrue(0 == map1.size());

        final Map<String, Integer> map2 = map("one", 1);
        assertNotNull(map2);
        assertTrue(1 == map2.size());
        assertEquals(new Integer(1), map2.get("one"));

        final Map<String, Integer> map3 = map("one", 1, "two", 2);
        assertNotNull(map3);
        assertTrue(2 == map3.size());
        assertEquals(new Integer(1), map3.get("one"));
        assertEquals(new Integer(2), map3.get("two"));

        final Map<String, Integer> map4 = map("one", 1, "two", 2, "three", 3);
        assertNotNull(map4);
        assertTrue(3 == map4.size());
        assertEquals(new Integer(1), map4.get("one"));
        assertEquals(new Integer(2), map4.get("two"));
        assertEquals(new Integer(3), map4.get("three"));

        final Map<String, Integer> map5 = map("one", 1, "two", 2, "three", 3, "four", 4);
        assertNotNull(map5);
        assertTrue(4 == map5.size());
        assertEquals(new Integer(1), map5.get("one"));
        assertEquals(new Integer(2), map5.get("two"));
        assertEquals(new Integer(3), map5.get("three"));
        assertEquals(new Integer(4), map5.get("four"));

        final Map<String, Integer> map6 = map("one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
        assertNotNull(map6);
        assertTrue(5 == map6.size());
        assertEquals(new Integer(1), map6.get("one"));
        assertEquals(new Integer(2), map6.get("two"));
        assertEquals(new Integer(3), map6.get("three"));
        assertEquals(new Integer(4), map6.get("four"));
        assertEquals(new Integer(5), map6.get("five"));

    }

    @Test
    public void testRemoveKey(){
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        CollectionsUtils.renameKey( map, "key1", "key2");
        assertEquals( map.get("key1"), null);
        assertEquals( map.get("key2"), "value1");
    }

    @Test
    public void testRemoveDontExistKey(){
        Map<String, String> map = new HashMap<>();
        CollectionsUtils.renameKey( map, "key1", "key2");
        assertEquals( map.get("key1"), null);
        assertEquals( map.get("key2"), null);
    }
}