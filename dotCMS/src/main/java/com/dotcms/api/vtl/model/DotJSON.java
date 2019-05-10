package com.dotcms.api.vtl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DotJSON implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public static final String CACHE_TTL_KEY = "dotcache";
    @JsonIgnore
    private LocalDateTime cachedSince;

    private final Map<String, Object> map = new HashMap<>();

    public void put(final String key, final Object value) {
        this.map.put(key, value);
    }

    public Object get(final String key) {
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
                cacheTTL = (int) this.map.get(CACHE_TTL_KEY);
            } catch (ClassCastException e) {
                Logger.warn(this, "Unable to parse Cache TTL Value in DotJSON Object. " +
                        "Trying parsing as string now.");
                try {
                    cacheTTL = Integer.parseInt((String) this.map.get(CACHE_TTL_KEY));
                } catch(NumberFormatException nfe) {
                    Logger.error(this, "Unable to parse Cache TTL Value in DotJSON Object. Returning" +
                            "default cacheTTL value '0'");
                }
            }
        }

        return cacheTTL;
    }

    public Map<String, Object> getMap() {
        return map;
    }
}
