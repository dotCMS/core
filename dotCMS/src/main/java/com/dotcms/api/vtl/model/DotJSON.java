package com.dotcms.api.vtl.model;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DotJSON<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public static final String CACHE_TTL_KEY = "cache";
    @JsonIgnore
    private LocalDateTime cachedSince;

    private Map<K, V> map = new HashMap<>();

    public void put(final K key, final V value) {
        this.map.put(key, value);
    }

    public V get(final K key) {
        return this.map.get(key);
    }

    public int size() {
        return this.map.size();
    }

    @JsonIgnore
    public LocalDateTime getCachedSince() {
        return cachedSince;
    }

    public void setCachedSince(final LocalDateTime cachedSince) {
        this.cachedSince = cachedSince;
    }

    @JsonIgnore
    public int getCacheTTL() {
        Integer cacheTTL = 0;

        if(UtilMethods.isSet(this.map.get(CACHE_TTL_KEY))) {
            try {
                cacheTTL = (Integer) this.map.get(CACHE_TTL_KEY);
            } catch (ClassCastException e) {
                Logger.error(this, "Unable to parse Cache TTL Value in DotJSON Object");
            }
        }

        return cacheTTL;
    }

    public Map<K, V> getMap() {
        return map;
    }
}
