package com.dotcms.util;

import java.io.Serializable;
import java.util.*;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Encapsulates collection utils methods
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 8, 2016
 */
@SuppressWarnings("serial")
public class CollectionsUtils implements Serializable {

	/**
	 * Returns the object in the {@link Map} specified by its key. If it doesn't
	 * exist, returns its default value.
	 * 
	 * @param map
	 *            - The map containing the elements to inspect.
	 * @param key
	 *            - The key of the value to return.
	 * @param defaultValue
	 *            - The default value to return in case the key doesn't match a
	 *            value in the map.
	 * @return The value mapped to the specified key, or the default value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getMapValue(Map<?, ?> map, Object key, T defaultValue) {
		if (!map.containsKey(key)) {
			return defaultValue;
		} else {
			return (T) map.get(key);
		}
	}

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
     * Get a new {@link LinkedHashSet} with the elements
     * @param elements T
     * @param <T>
     * @return Set
     */
    public static <T> Set<T> linkSet (final T... elements) {

        return new LinkedHashSet<T>(Arrays.asList(elements));
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


    public static <K,V> Map<K,V> imap(Object... entries) {

        if (entries.length % 2 != 0){
            throw new IllegalArgumentException("The entries must be pair");
        }

        Collection<Map.Entry<K, V>> entriesCollection = new ArrayList<>();

        for (int i = 0; i < entries.length; i += 2) {
            entriesCollection.add(entry((K) entries[i], (V) entries[i + 1]));
        }

        return mapEntries(entriesCollection);
    } // map.

    private static <K,V> Map<K,V> immutableMapEntries(final Collection<Map.Entry<K, V>> entries) {

        final ImmutableMap.Builder<K, V> parametersBuilder = ImmutableMap.builder();

        for (Map.Entry<K, V> entry : entries) {
            parametersBuilder.put(entry.getKey(), entry.getValue());
        }

        return parametersBuilder.build();
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

    /**
     * Rename key1 by key2 in map, for example, if We have the follow code:<br>
     *     <pre>
     *         Map<Strin, String> map = new HashMap<>();
     *         map.put("key1", "value1");
     *         CollectionsUtils.renameKey( map, "key1", "key2");
     *         System.out.println( map.get("key1") + " " + map.get("key2"));
     *     </pre>
     *
     * The output will be <pre>value1 null</pre>
     * 
     * @param map map where the key have to be replace
     * @param key1 key to replace
     * @param key2 new key
     * @param <K>
     * @param <V>
     */
    public static <K, V> void renameKey (Map<K, V> map, K key1, K key2){
        V emailaddress = map.get( key1 );
        map.remove( key1 );
        map.put( key2, emailaddress);
    }

} // E:O:F:CollectionsUtils.
