package com.dotcms.util;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public class MapWrapper<K,V> implements Map<K,V>, Serializable {

    private final Map<K,V> delegate;


    public MapWrapper(final Map<K, V> delegate) {
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
    public boolean containsKey(final Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        return this.delegate.get(key);
    }

    @Nullable
    @Override
    public V put(final K key, final V value) {
        return delegate.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(@NotNull final Map<? extends K, ? extends V> m) {
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
