package com.ettrema.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Have to sychronise on everything, even gets, because we're using LRU ordering.
 * See the LinkedHashMap javadoc
 * 
 *
 * @author brad
 */
public class OrderedMap<K, T> {

    private final LinkedHashMap<K, T> map = new LinkedHashMap<K, T>( 1000, 0.75f, true );

    public synchronized T get( K key ) {
        return map.get( key );
    }

    public synchronized T remove( K key ) {
        return map.remove( key );
    }

    public synchronized void put( K key, T val ) {
        map.put( key, val );
    }

    public synchronized Map.Entry<K, T> removeFirst() {
        Iterator<K> it = map.keySet().iterator();
        K key = null;
        while( it.hasNext() ) {
            key = it.next();
            break;
        }
        if( key != null ) {
            final K k2 = key;
            final T val = map.remove( key );

            return new Map.Entry<K, T>() {

                @Override
                public K getKey() {
                    return k2;
                }

                @Override
                public T getValue() {
                    return val;
                }

                @Override
                public T setValue( T value ) {
                    throw new UnsupportedOperationException( "Not supported." );
                }

            };
        } else {
            return null;
        }
    }

    public synchronized void clear() {
        map.clear();
    }

    public int size() {
        return map.size();
    }
}
