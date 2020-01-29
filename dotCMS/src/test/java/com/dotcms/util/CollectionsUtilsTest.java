package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotcms.workflow.form.AssignCommentBean;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.*;
import static org.junit.Assert.*;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class CollectionsUtilsTest extends UnitTestBase {

    @Test
    public void find() {

        final List<AssignCommentBean>     arrays = Arrays.asList(new AssignCommentBean("1","1"),
                new AssignCommentBean("2","2"), new AssignCommentBean("3","3"), new AssignCommentBean("4","4"),
                new AssignCommentBean("5","5"), new AssignCommentBean("6","6"), new AssignCommentBean("7","7"));
        final Optional<AssignCommentBean> match1  = CollectionsUtils.find(arrays, assignCommentBean -> assignCommentBean.getAssign().equals("6"));
        Assert.assertTrue(match1.isPresent());
        Assert.assertEquals("6", match1.get().getAssign());
        Assert.assertEquals("6", match1.get().getComment());

        final Optional<AssignCommentBean> match2  = CollectionsUtils.find(arrays, assignCommentBean -> assignCommentBean.getAssign().equals("8"));
        Assert.assertFalse(match2.isPresent());
    }

    @Test
    public void join_null() {

        final String string = CollectionsUtils.join(null, null, null);
        Assert.assertEquals(StringPool.BLANK, string);
    }

    @Test
    public void join_empty() {

        final String string = CollectionsUtils.join(StringPool.BLANK, StringPool.BLANK, Collections.emptyList());
        Assert.assertEquals(StringPool.BLANK, string);
    }

    @Test
    public void join_one_element() {

        final String string = CollectionsUtils.join(StringPool.COMMA, "one", Collections.emptyList());
        Assert.assertEquals("one", string);
    }

    @Test
    public void join_elements() {

        final String string = CollectionsUtils.join(StringPool.COMMA, "one", Arrays.asList("two","three","four"));
        Assert.assertEquals("one,two,three,four", string);
    }

    @Test
    public void classified_by_id_computeSubValueIfAbsentTest() {

        final List<Tuple2<Integer, String>> unclassifiedList = CollectionsUtils.list(
                Tuple.of(1, "a"), Tuple.of(1, "b"), Tuple.of(1, "c"), Tuple.of(1, "d"), Tuple.of(1, "e"), // 5
                Tuple.of(2, "f"), Tuple.of(2, "g"), Tuple.of(2, "h"), Tuple.of(2, "i"), Tuple.of(2, "j"), Tuple.of(2, "k"), // 6
                Tuple.of(3, "l"), Tuple.of(3, "m"), Tuple.of(3, "n"), Tuple.of(3, "o"), Tuple.of(3, "p"), Tuple.of(3, "q"), Tuple.of(3, "r")); // 7
        final Map<Integer, List<String>> integerListMap = new HashMap<>();

        for (final Tuple2<Integer, String> tuple2 : unclassifiedList) {

            final Integer key = tuple2._1;
            final String  abc = tuple2._2;
            CollectionsUtils.computeSubValueIfAbsent(integerListMap, key, abc,
                    (List<String> currentList, String abcLetter)-> CollectionsUtils.add(currentList, abcLetter),
                    (Integer intKey, String abcLetter)-> CollectionsUtils.list(abcLetter));
        }


        Assert.assertFalse(integerListMap.isEmpty());

        Assert.assertEquals(3, integerListMap.size());
        Assert.assertEquals(5, integerListMap.get(1).size());
        Assert.assertEquals(6, integerListMap.get(2).size());
        Assert.assertEquals(7, integerListMap.get(3).size());

        Assert.assertFalse(integerListMap.get(1).contains("h"));
        Assert.assertTrue(integerListMap.get(1).contains("e"));

        Assert.assertFalse(integerListMap.get(2).contains("o"));
        Assert.assertTrue(integerListMap.get(2).contains("j"));

        Assert.assertFalse(integerListMap.get(3).contains("a"));
        Assert.assertTrue(integerListMap.get(3).contains("l"));
    }

    @Test
    public void merge_current() {

        final List<Tuple2<String, Integer>> list = Arrays.asList(Tuple.of("hello", 1), Tuple.of("hello", 2),
                Tuple.of("hi", 3), Tuple.of("yeah", 4), Tuple.of("hi", 5), Tuple.of("hello", 6));
        final Map<String, Integer> map = list
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple._1,
                        tuple -> tuple._2,
                        Merge.current()
                ));

        assertEquals(3, map.size()); // 2, 5 and 6 would skipped. 1,3 and 4 will be part of it
        assertEquals(1, (int)map.get("hello")); // 2, 5 and 6 would skipped. 1,3 and 4 will be part of it
        assertEquals(3, (int)map.get("hi")); // 2, 5 and 6 would skipped. 1,3 and 4 will be part of it
        assertEquals(4, (int)map.get("yeah")); // 2, 5 and 6 would skipped. 1,3 and 4 will be part of it
        assertNotEquals(2, (int)map.get("hello"));
        assertNotEquals(5, (int)map.get("hi")); // 2, 5 and 6 would skipped. 1,3 and 4 will be part of it
        assertNotEquals(6, (int)map.get("hello")); // 2, 5 and 6 would skipped. 1,3 and 4 will be part of it
    }

    @Test
    public void merge_last() {

        final List<Tuple2<String, Integer>> list = Arrays.asList(Tuple.of("hello", 1), Tuple.of("hello", 2),
                Tuple.of("hi", 3), Tuple.of("yeah", 4), Tuple.of("hi", 5), Tuple.of("hello", 6));
        final Map<String, Integer> map = list
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple._1,
                        tuple -> tuple._2,
                        Merge.last()
                ));

        assertEquals(3, map.size());
        assertEquals(6, (int)map.get("hello"));
        assertEquals(5, (int)map.get("hi"));
        assertEquals(4, (int)map.get("yeah"));
        assertNotEquals(1, (int)map.get("hello"));
        assertNotEquals(3, (int)map.get("hi"));
        assertNotEquals(2, (int)map.get("hello"));
    }

    @Test
    public void merge_compare() {

        final List<Tuple2<String, Integer>> list = Arrays.asList(Tuple.of("hello", 10), Tuple.of("hello", 2),
                Tuple.of("hi", 33), Tuple.of("yeah", 4), Tuple.of("hi", 5), Tuple.of("hello", 6));
        final Map<String, Integer> map = list
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple._1,
                        tuple -> tuple._2,
                        Merge.compare((current, last)-> Math.max(current, last))
                ));

        assertEquals(3, map.size());
        assertEquals(10, (int)map.get("hello"));
        assertEquals(33, (int)map.get("hi"));
        assertEquals(4, (int)map.get("yeah"));
        assertNotEquals(1, (int)map.get("hello"));
        assertNotEquals(3, (int)map.get("hi"));
        assertNotEquals(2, (int)map.get("hello"));
    }

    @Test
    public void groupByKeyTest()  {

        final List<String> list = Arrays.asList("hello","hello","hi","yeah","hi","hello");

        assertNotNull(list);

        final Map<String, List<String>> groupByKeyMap = groupByKey(list, v -> v);

        assertNotNull(groupByKeyMap);
        assertEquals(3, groupByKeyMap.size());
        assertEquals(3, groupByKeyMap.get("hello").size());
        assertEquals(2, groupByKeyMap.get("hi").size());
        assertEquals(1, groupByKeyMap.get("yeah").size());

    }


    @Test
    public void partitionsTest()  {

        final List<List<String>> listOfList = partition (Arrays.asList("hello","hello","hi","yeah","hi","hello"),
                (s) -> s.equals("hi"), (s) -> s.equals("hello"), (s) -> s.equals("yeah"));

        assertNotNull(listOfList);
        assertEquals(3, listOfList.size());
        assertEquals(Arrays.asList("hi", "hi"), listOfList.get(0));
        assertEquals(Arrays.asList("hello", "hello","hello"), listOfList.get(1));
        assertEquals(Arrays.asList("yeah"), listOfList.get(2));
    }

    @Test
    public void partitions_not_matchTest()  {

        final List<List<String>> listOfList = partition (Arrays.asList("hello","hello","hi","yeah","hi","hello"),
                (s) -> s.equals("blabla"), (s) -> s.equals("blablabla"), (s) -> s.equals("blablablabla"));

        assertNotNull(listOfList);
        assertEquals(3, listOfList.size());
        assertNotNull(listOfList.get(0));
        assertTrue(listOfList.get(0).isEmpty());
        assertNotNull(listOfList.get(1));
        assertTrue(listOfList.get(1).isEmpty());
        assertNotNull(listOfList.get(2));
        assertTrue(listOfList.get(2).isEmpty());

    }

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