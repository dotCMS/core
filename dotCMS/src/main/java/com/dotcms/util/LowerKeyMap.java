package com.dotcms.util;


import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LowerKeyMap<V> extends MapWrapper<String, V> implements Map<String, V>, Serializable {

    private static final long serialVersionUID = 1L;

    public LowerKeyMap() {
        this(new HashMap<>());
    }

    public LowerKeyMap(final Map<String, V> delegate) {
        super(delegate);
    }

    public LowerKeyMap(final Map<String, V> delegate, final Map<? extends String, ? extends V> baseMap) {
        super(delegate);
        this.putAll(baseMap);
    }

    @Override
    public V put(final String key, final V value) {
        return super.put(key.toLowerCase(), value);
    }

    @Override
    public final void putAll(@NotNull final Map<? extends String, ? extends V> m) {

        for (final Map.Entry<? extends String, ? extends V> e : m.entrySet()) {

            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V get(final Object key) {
        return super.get(String.class.cast(key).toLowerCase());
    }
}
