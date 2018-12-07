package com.dotcms.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapWrapper<K,V> implements Map<K,V> {

    private final Map<K,V> delegate;


    public MapWrapper(Map<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.delegate.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return delegate.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }
}
