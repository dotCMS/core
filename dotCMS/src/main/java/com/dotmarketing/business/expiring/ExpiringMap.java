package com.dotmarketing.business;

/**
 *
 * @author jsanca
 */
public interface ExpiringMap<K, V> {

    boolean containsKey(K key);
    V remove(K key);
    V get(K key);

} // E:O:F:ExpiringMap.
