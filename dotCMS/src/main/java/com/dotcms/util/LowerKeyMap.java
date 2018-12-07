package com.dotcms.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class LowerKeyMap<V> extends MapWrapper<String, V> implements Map<String, V> {

    public LowerKeyMap() {
        this(new HashMap<>());
    }

    public LowerKeyMap(Map<String, V> delegate) {
        super(delegate);
    }

    public LowerKeyMap(Map<String, V> delegate, Map<? extends String, ? extends V> baseMap) {
        super(delegate);
        this.putAll(baseMap);
    }

    @Override
    public V put(String key, V value) {
        return super.put(key.toLowerCase(), value);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends V> m) {

        for (final Map.Entry<? extends String, ? extends V> e : m.entrySet()) {

            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V get(Object key) {
        return super.get(String.class.cast(key).toLowerCase());
    }
}
