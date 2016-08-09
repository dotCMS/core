package com.dotcms.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Encapsulates collection utils methods
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 8, 2016
 */
public class CollectionsUtils implements Serializable {

    /**
     * Get a new empty list
     * @param <T>
     * @return List
     */
    public static <T> List<T> getNewList() { // this method is keep for compatibility

        return list();
    } // getNewList

    /**
     * Get a new empty list
     *
     * Example:
     * final List<String> list1 = list();
     *
     * @param <T>
     * @return List
     */
    public static <T> List<T> list() {

        return new ArrayList<T>();
    } // list


    /**
     * Get a new list with the elements
     *
     * Example:
     * final List<Integer> list2 = list(1, 2, 3, 4, 5);
     *
     *
     * @param elements T
     * @param <T>
     * @return List
     */
    public static <T> List<T> list(final T... elements) {

        return list(Arrays.asList(elements));
    } // list

    /**
     * Get a new list based on a collection
     * @param tCollection {@link Collection}
     * @param <T>
     * @return List
     */
    public static <T> List<T> list(final Collection<T> tCollection) {

        return new ArrayList<T>(tCollection);
    } // list

    /**
     * Get a new set
     * @param <T>
     * @return Set
     */
    public static <T> Set<T> set () {

        return new HashSet<T>();
    } // set

    /**
     * Get a new set with the elements
     * @param elements T
     * @param <T>
     * @return Set
     */
    public static <T> Set<T> set (final T... elements) {

        return set(Arrays.asList(elements));
    } // set

    /**
     * Get a new set
     * @param tCollection {@link Collection}
     * @param <T>
     * @return Set
     */
    public static <T> Set<T> set (final Collection<T> tCollection) {

        return new HashSet<T>(tCollection);
    } // set

    /**
     * Get a new map
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map() {

        return new HashMap<K, V>();
    } // map.

    /**
     * Create a new map adding an array maps to it
     * @param maps {@link Map}
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> mapAll(final Map<K, V>... maps) {

        final Map<K,V> map = map();

        if (null != maps) {
            for (Map<K, V> mapItem : maps) {
                map.putAll(mapItem);
            }
        }

        return map;
    } // map.

    /**
     * Get a new map based on a key and value
     * @param key K
     * @param value V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key, final V value) {

        return mapEntries(entry(key, value));
    } // map.

    /**
     * Get a new map based on a pair of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2) {

        return mapEntries(entry(key1, value1), entry(key2, value2));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param key5 K
     * @param value5 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4, final K key5, final V value5) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4), entry(key5, value5));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param key5 K
     * @param value5 V
     * @param key6 K
     * @param value6 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4, final K key5, final V value5,
            final K key6, final V value6) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4), entry(key5, value5),
                entry(key6, value6));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param key5 K
     * @param value5 V
     * @param key6 K
     * @param value6 V
     * @param key7 K
     * @param value7 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4, final K key5, final V value5,
                                     final K key6, final V value6, final K key7, final V value7) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4), entry(key5, value5),
                entry(key6, value6), entry(key7, value7));
    } // map.


    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param key5 K
     * @param value5 V
     * @param key6 K
     * @param value6 V
     * @param key7 K
     * @param value7 V
     * @param key8 K
     * @param value8 V
     * @param key9 K
     * @param value9 V
     * @param key10 K
     * @param value10 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4, final K key5, final V value5
            , final K key6, final V value6, final K key7, final V value7, final K key8, final V value8
            , final K key9, final V value9, final K key10, final V value10) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4), entry(key5, value5),
                entry(key6, value6), entry(key7, value7), entry(key8, value8),
                entry(key9, value9), entry(key10, value10));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param key5 K
     * @param value5 V
     * @param key6 K
     * @param value6 V
     * @param key7 K
     * @param value7 V
     * @param key8 K
     * @param value8 V
     * @param key9 K
     * @param value9 V
     * @param key10 K
     * @param value10 V
     * @param key11 K
     * @param value11 V
     * @param key12 K
     * @param value12 V
     * @param key13 K
     * @param value13 V
     * @param key14 K
     * @param value14 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4, final K key5, final V value5
            , final K key6, final V value6, final K key7, final V value7, final K key8, final V value8
            , final K key9, final V value9, final K key10, final V value10
            , final K key11, final V value11, final K key12, final V value12, final K key13, final V value13
            , final K key14, final V value14) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4), entry(key5, value5),
                entry(key6, value6), entry(key7, value7), entry(key8, value8),
                entry(key9, value9), entry(key10, value10), entry(key11, value11),
                entry(key12, value12), entry(key13, value13), entry(key14, value14));
    } // map.

    /**
     * Get a new map based on a list of key/value.
     * @param key1 K
     * @param value1 V
     * @param key2 K
     * @param value2 V
     * @param key3 K
     * @param value3 V
     * @param key4 K
     * @param value4 V
     * @param key5 K
     * @param value5 V
     * @param key6 K
     * @param value6 V
     * @param key7 K
     * @param value7 V
     * @param key8 K
     * @param value8 V
     * @param key9 K
     * @param value9 V
     * @param key10 K
     * @param value10 V
     * @param key11 K
     * @param value11 V
     * @param key12 K
     * @param value12 V
     * @param key13 K
     * @param value13 V
     * @param key14 K
     * @param value14 V
     * @param key15 K
     * @param value15 V
     * @param key16 K
     * @param value16 V
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> map(final K key1, final V value1, final K key2, final V value2
            , final K key3, final V value3, final K key4, final V value4, final K key5, final V value5
            , final K key6, final V value6, final K key7, final V value7, final K key8, final V value8
            , final K key9, final V value9, final K key10, final V value10
            , final K key11, final V value11, final K key12, final V value12, final K key13, final V value13
            , final K key14, final V value14, final K key15, final V value15, final K key16, final V value16) {

        return mapEntries(entry(key1, value1), entry(key2, value2),
                entry(key3, value3), entry(key4, value4), entry(key5, value5),
                entry(key6, value6), entry(key7, value7), entry(key8, value8),
                entry(key9, value9), entry(key10, value10), entry(key11, value11),
                entry(key12, value12), entry(key13, value13), entry(key14, value14),
                entry(key15, value15), entry(key16, value16));
    } // map.



    /**
     * Get a new map based on a collections of entries
     * @param entries Entry
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> mapEntries(final Map.Entry<K, V>... entries) {

        final Map<K,V> hashMap = map();

        for (Map.Entry<K, V> entry : entries) {

            hashMap.put(entry.getKey(), entry.getValue());
        }

        return hashMap;
    } // map.

    /**
     * Get a new map based on a collections of entries
     * @param entries Collection
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> mapEntries(final Collection<Map.Entry<K, V>> entries) {

        final Map<K,V> hashMap = map();

        for (Map.Entry<K, V> entry : entries) {

            hashMap.put(entry.getKey(), entry.getValue());
        }

        return hashMap;
    } // map.

    /**
     * Get an entry base on a key and value
     * @param key K
     * @param value V
     * @param <K>
     * @param <V>
     * @return Map.Entry
     */
    public static <K,V> Map.Entry<K, V> entry(final K key, final V value) {

        return new AbstractMap.SimpleEntry<K, V>(key, value);
    } // entry

} // E:O:F:CollectionsUtils.
