package com.dotcms.business.expiring;

import io.vavr.Tuple2;

import java.util.concurrent.TimeUnit;

/**
 * An {@link ExpiringEntryStrategy} provides an specific timeout to remove an entry from the  {@link ExpiringMap}
 * @param <K>
 * @param <V>
 */
@FunctionalInterface
public interface ExpiringEntryStrategy<K, V> {

    /**
     * This method will receives the entry as a Key and value; expecting a Tuple with a time in long value and the timeunit that represents,
     * for instance if you want to return tree seconds in millis you can do something like
     * <pre>
     *     ExpiringEntryStrategy<MyObject, MyValue> strategy = (key, value)-> Tuple.of(3000, TimeUnit.MILLISECONDS);
     * </pre>
     *
     * Or do something more complicated such as depending on the type of key, return some timeout or another, etc.
     *
     * @param key K
     * @param value V
     * @return Tuple2 with the time and their unit
     */
    Tuple2<Long, TimeUnit> getExpireTime(K key, V value);
}
