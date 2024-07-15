package com.dotcms.util;

import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.MapBuilder;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * This utility class provides common use methods for creating and interacting
 * with Java collections, such as maps, lists, sets, and so on. It is advised
 * that any utility methods regarding interaction with theses data structures be
 * added to this class.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 8, 2016
 */
@SuppressWarnings("serial")
public class CollectionsUtils implements Serializable {

    /**
     * Group the list collection based on a key function, grouping all the items with the same key into a sub list
     *
     * For instance if you have a list such as
     * [1, 2, 3, 1, 3, 4, 5] and the key function is v -> v,
     * that means the same value is the key, but you can might have an object and return the id for instance myentity -> myentity.getId(), it will group by id, etc.
     *
     * it will return:
     * 1 -> [1, 1]
     * 2 -> [2]
     * 3 -> [3, 3]
     * 4 -> [4]
     * 5 -> [5]
     *
     * @param list
     * @param keyFunction
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K,V> Map<K, List<V>> groupByKey (final List<V> list, final Function<V, K> keyFunction) {

        final Map<K, List<V>> listGroupByKey = new HashMap<>();

        list.stream().forEach(v ->
                listGroupByKey.computeIfAbsent(keyFunction.apply(v),
                        k -> new ArrayList<>()).add(v));

        return listGroupByKey;
    }

    /**
     * Split out the collection in ArrayList of ArrayList that satisfied each Predicate
     * it means all the items on collections that match the predicate 0 for instance will be stored in the first list of the final returned list, for instance:
     *
     * <pre>
     *
     *
     *     List listOfList = partition (Arrays.asList("hello","hello","hi","yeah","hi","hello", (s) -> s.equals("hi"), (s) -> s.equals("hello"), (s) -> s.equals("yeah"))
     *
     *     System.out.println(listOfList.get(0));  // will print hi, hi
     *     System.out.println(listOfList.get(1));  // will print hello, hello,hello
     *     System.out.println(listOfList.get(2));  // will print yeah
     * </pre>
     *
     * If there is not any match for a predicate, the corresponding list will be returned empty
     * @param collection
     * @param predicates
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> partition(final Collection<T> collection, final Predicate<T>... predicates) {

        final List<List<T>> partitions = new ArrayList<>(predicates.length);
        fill(partitions, predicates.length, CollectionsUtils::list);

        for (final T item : collection) {

            for (int i = 0; i < predicates.length; ++i) {

                final Predicate<T> predicate = predicates[i];
                if (predicate.test(item)) {

                    partitions.get(i).add(item);
                }
            }
        }

        return partitions;
    } // partition.

    /**
     * Fills the collection will the supplier
     * @param collection {@link Collection}
     * @param <T>
     */
    public static <T> void fill(final Collection<T> collection, final int size, final Supplier<T> supplier) {

        for (int i = 0; i < size; i++) {

            collection.add(supplier.get());
        }
    } // fillList.

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
     * This method is pretty useful when you have a collection as a value inside a map, for instance: <br/>
     *
     * Map<String, List<Contentlet>> mapOfLists = ....
     *
     * Lets say you have the Contentlet value called "content" and the key called "identifier"
     * usually you will have to see if the mapOfList contains the key identifier (the idea behind is the classified a subsets of contentlets by key).
     * Then if contains the key, get the list and then add the content value to the list.
     * Otherwise, you will have to create the list, add the content value and put into the map.
     *
     * With this method you can do all of these in just one line, such as
     *
     * Remember identifier is the key of the Map, content is the value to accumulate in the list.
     *
     * <pre>
     * computeSubValueIfAbsent (mapOfLists, identifier, content,
     *                          (List<Contentlet> currentList, Contentlet content)-> CollectionsUtils.add(currentList, content),
     *                          (String key, Contentlet content)-> CollectionsUtils.list(content));
     *</pre>
     *
     * So the first parameter is the Map with the list of contentlets as a value.
     * The second parameter will be the key to get the value associated to that key.
     * The third one will be in this case the sub value to add to the existing or new list.
     * The next is the a {@link BiFunction}, this function is to performs the subvalue when the value already exists in the map;
     * it expects the current value, the subvalue and will return the new current value, in our example will receive a current list, and them adds and returns the list plus the new subvalue.
     *
     * Finally, there is the {@link BiFunction} when there is not value associated to the key, in this case will receive the key and the subvalue and will expect
     * a new value to be added to the map, performing at the same time the current subvalue; in our example it is creating a new ArrayList with the content value inside it.
     *
     * @param map
     * @param key
     * @param subValue
     * @param mappingSubValueOnCurrentValueFunction
     * @param mappingFunction
     * @param <K>
     * @param <V>
     * @param <SV>
     * @return V
     */
	public static <K,V, SV>  V computeSubValueIfAbsent (final Map<K, V> map, final K key,
                                                     final SV subValue,
                                                     final BiFunction<V,SV,V> mappingSubValueOnCurrentValueFunction,
                                                     final BiFunction<? super K,SV,V> mappingFunction) {

        V value;
        if ((value = map.get(key)) == null) { // if accumulator value does not exists

            final V newValue = mappingFunction.apply(key, subValue);
            map.put(key, newValue);
            return newValue;
        }

        // if value exists compute the sub value on it
        return mappingSubValueOnCurrentValueFunction.apply(value, subValue);
    } // computeSubValueIfAbsent.

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
        return new ArrayList<>();
    } // list


    /**
     * This method just adds several values to the list and return the list to continue a chain
     * @param list  {@link List}
     * @param values T array of values
     * @param <T>
     * @return the List with the old and new values
     */
    public static <T> List<T> add(final List<T> list,  final T... values) {

        for (final T value : values) {
            list.add(value);
        }
        return list;
    }

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
	@SuppressWarnings("unchecked")
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
        return new ArrayList<>(tCollection);
    } // list

    /**
     * Get a new set
     * @param <T>
     * @return Set
     */
    public static <T> Set<T> set () {
        return new HashSet<>();
    } // set

    /**
     * Get a new set with the elements
     * @param elements T
     * @param <T>
     * @return Set
     */
    @SuppressWarnings("unchecked")
	public static <T> Set<T> set (final T... elements) {
        return set(Arrays.asList(elements));
    } // set

    /**
     * Get a new {@link LinkedHashSet} with the elements
     * @param elements T
     * @param <T>
     * @return Set
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> linkSet (final T... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    } // set

    /**
     * Get a new set
     * @param tCollection {@link Collection}
     * @param <T>
     * @return Set
     */
    public static <T> Set<T> set (final Collection<T> tCollection) {
        return new HashSet<>(tCollection);
    } // set

    /**
     * Create a new map adding an array maps to it
     * @param maps {@link Map}
     * @param <K>
     * @param <V>
     * @return Map
     */
    @SafeVarargs
    public static <K,V> Map<K,V> mapAll(final Map<K, V>... maps) {
        final Map<K,V> map = new HashMap<>();

        if (null != maps) {
            for (Map<K, V> mapItem : maps) {
                map.putAll(mapItem);
            }
        }

        return map;
    } // mapAll.

    /**
     * Returns an immutable map based on the objects entries (must be pairs otherwise will throws an {@link IllegalArgumentException})
     * @param entries Object an array
     * @param <K>
     * @param <V>
     * @return Map
     */
    @SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> imap(Object... entries) {
        if (entries.length % 2 != 0){
            throw new IllegalArgumentException("The entries must be pair");
        }

        Collection<Map.Entry<K, V>> entriesCollection = new ArrayList<>();

        for (int i = 0; i < entries.length; i += 2) {
            entriesCollection.add(entry((K) entries[i], (V) entries[i + 1]));
        }

        return imapEntries(entriesCollection);
    } // map.

    /**
     * Get a new map based on a collections of entries
     * @param entries Entry
     * @param <K>
     * @param <V>
     * @return Map
     */
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> mapEntries(final Map.Entry<K, V>... entries) {
        final Map<K,V> hashMap = new HashMap<>();

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
        final Map<K,V> hashMap = new HashMap<>();

        for (Map.Entry<K, V> entry : entries) {

            hashMap.put(entry.getKey(), entry.getValue());
        }

        return hashMap;
    } // map.

    /**
     * Get a new immutable map based on a collections of entries
     * @param entries Collection
     * @param <K>
     * @param <V>
     * @return Map
     */
    public static <K,V> Map<K,V> imapEntries(final Collection<Map.Entry<K, V>> entries) {
        final MapBuilder<K,V> hashMap = new MapBuilder<>();

        for (Map.Entry<K, V> entry : entries) {

            hashMap.put(entry.getKey(), entry.getValue());
        }

        return hashMap.immutableMap();
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
        return new AbstractMap.SimpleEntry<>(key, value);
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

	/**
	 * Returns the first object in the {@link Map} whose key <b>is represented
	 * by or starts with some of the characters of the specified seed</b>. This
	 * method allows developers to simulate a reverse {@code startsWith}
	 * operation in a {@code Map}. That is, the map key only contains some of
	 * the initial characters in the seed.
	 * <p>
	 * If the map contains a complete key represented by the {@code seed}
	 * parameter, such a value will be returned. Otherwise, the method will go
	 * trough all the map entries looking for the first key whose characters
	 * match the specified seed. If none of the keys match the seed in any way,
	 * the default value will be returned. For example:
	 * 
	 * <pre>
	 * String seed = "prefix_mapentry";
	 * Map<String, String> map = new HashMap<>();
	 * 
	 * map.put("first.key", "1");
	 * map.put("prefix_", "2");
	 * map.put("third.key", "3");
	 * 
	 * System.out.println(getMapValue(map, seed, null));
	 * </pre>
	 * 
	 * The following value will be printed:
	 * 
	 * <pre>
	 * 2
	 * </pre>
	 * 
	 * @param map
	 *            - The map containing the elements to inspect.
	 * @param seed
	 *            - The key of the value to return.
	 * @param defaultValue
	 *            - The default value to return in case the key doesn't match an
	 *            entry in the map.
	 * @return The value mapped to the specified key, or the default value.
	 */
	public static <T> T getMapValue(final Map<String, T> map, final String seed, T defaultValue) {
		if (map.containsKey(seed)) {
			return map.get(seed);
		} else {
			for (Map.Entry<String, T> entry : map.entrySet()) {
				if (seed.startsWith(entry.getKey())) {
					return entry.getValue();
				}
			}
		}
		return defaultValue;
	}

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@link ImmutableList}.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which collects all the input elements into immutable list
     */
	public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList () {
	    return new ImmutableListCollector<>();
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@link ImmutableSet}.
     *
     * @param collections
     * @return
     * @param <T>
     */
    public static <T> Collection<T> addAll(Collection<T>... collections) {
        final List<T> result = new ArrayList<>();
        for (Collection<T> collection : collections) {
            result.addAll(collection);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Took a non serializable map and convert it and all the contents inside to a serializable map
     * Note: if a value is a map, use recursive to create that inner map to a serializable map (if it is not already)
     * if the value is not a map, but it is not serializable, then convert it to a string by calling toString
     * any null value will be skipped.
     *
     * @param nonSerializableMap Map
     * @return Serializable HashMap serializable
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static HashMap<Serializable, Serializable> toSerializableMap (final Map nonSerializableMap) {

        final HashMap<Serializable, Serializable> serializableMap = new HashMap<>();

        for (final Object key : nonSerializableMap.keySet()) {

            final Object value = nonSerializableMap.get(key);
            if (null != value) {
                if (value instanceof Map && !(value instanceof Serializable)) {

                    serializableMap.put((Serializable) key, toSerializableMap((Map) value));
                }else if (value instanceof List && !(value instanceof Serializable)) {

                    serializableMap.put((Serializable) key, toSerializableList((List) value));
                } else {

                    serializableMap.put((Serializable) key, value instanceof Serializable ? (Serializable) value : value.toString());
                }
            }
        }

        return serializableMap;
    }

    /**
     * Took a non serializable list and convert it and all the contents inside to a serializable arraylist
     * Note: if a value is a list, use recursive to create that inner list to a serializable list (if it is not already)
     * if the value is not a list, but it is not serializable, then convert it to a string by calling toString
     * any null value will be skipped.
     *
     * @param nonSerializableMap Map
     * @return Serializable HashMap serializable
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ArrayList<Serializable> toSerializableList(final List nonSerializableList) {

        final ArrayList<Serializable> serializableList = new ArrayList<>();

        for (final Object value : nonSerializableList) {

            if (null != value) {
                if (value instanceof Map && !(value instanceof Serializable)) {

                    serializableList.add(toSerializableMap((Map) value));
                }else if (value instanceof List && !(value instanceof Serializable)) {

                    serializableList.add(toSerializableList((List) value));
                } else {

                    serializableList.add(value instanceof Serializable ? (Serializable) value : value.toString());
                }
            }
        }

        return serializableList;
    }

    private static class ImmutableListCollector<T> implements Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> {
        @Override
        public Supplier<ImmutableList.Builder<T>> supplier() {
            return ImmutableList.Builder::new;
        }

        @Override
        public BiConsumer<ImmutableList.Builder<T>, T> accumulator() {
            return (b, e) -> b.add(e);
        }

        @Override
        public BinaryOperator<ImmutableList.Builder<T>> combiner() {
            return (b1, b2) -> b1.addAll(b2.build());
        }

        @Override
        public Function<ImmutableList.Builder<T>, ImmutableList<T>> finisher() {
            return ImmutableList.Builder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return ImmutableSet.of();
        }
    } // ImmutableListCollector.

    public static <T> List<T> asList (final Iterator<T> iterator) {
	    final List<T> result = new ArrayList<>();

        while(iterator.hasNext()){
            final T next = iterator.next();
            result.add(next);
        }

        return ImmutableList.copyOf(result);
    }

    public static String join (final String separator, final String firstString, final List<String> list) {

	    if (null == separator || null == firstString || null == list) {

	        return StringPool.BLANK;
        }

	    final List<String> listJoin = new ArrayList<>(list.size()+1);
	    listJoin.add(firstString);
	    listJoin.addAll(list);

        return StringUtils.join(listJoin, separator);
    }



    public static List join (final List ...lists) {

	    final List joinList = new ArrayList();

	    for (final Collection<?> list : lists) {

	        if (null != list) {
                joinList.addAll(list);
            }
        }

	    return joinList;
    }

    /**
     * Defines strategies for merging on collectors
      */
    public static class Merge {

        /**
         * Use this current value on the map, skipping the new one.
         * @param <T>
         * @return T
         */
        public static <T> BinaryOperator<T> current() {
            return (current,last) -> current;
        }

        /**
         * Discard the current value on the map in favor of the new one.
         * @param <T>
         * @return T
         */
        public static <T> BinaryOperator<T> last() {
            return (current,last) -> last;
        }

        /**
         * Use a comparator in order to decided witch one will be keep
         * The one defined as a greater on the comparison will be the one keep
         * @param comparator {@link Comparator}
         * @param <T>
         * @return T
         */
        public static <T> BinaryOperator<T> compare(final Comparator<T> comparator) {
            return (current,last) -> comparator.compare(current,last) >= 0 ? current : last;
        }

    }

    /**
     * Concatenates the contents of one array into the other one.
     *
     * @param array1 The base array used in the concatenation.
     * @param array2 The array that will be added to the first array.
     *
     * @return The resulting array
     */
    public static <T> T[] concat(final T[] array1, final T[] array2) {
        return Stream.concat(Arrays.stream(array1), Arrays.stream(array2))
                .toArray(size -> (T[]) Array.newInstance(array1.getClass().getComponentType(), size));
    }

    /**
     * Utility method that returns the non-null value, or its default value.
     *
     * @param value    The value to be checked.
     * @param fallback The fallback value in case the value is null.
     *
     * @return The value if it is not null, or the fallback value.
     */
    public static Object orElseGet(final Object value, final Object fallback) {
        return UtilMethods.isSet(value) ? value : fallback;
    }

}
