package com.dotcms.analytics.track.collectors;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a collector payload bean that is thread safe
 * @author jsanca
 */
public class ConcurrentCollectorPayloadBean implements CollectorPayloadBean {

    private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<>();

    public ConcurrentCollectorPayloadBean() {
        // empty
    }

    public ConcurrentCollectorPayloadBean(final Map<String, Serializable> customMap) {
        map.putAll(customMap);
    }

    @Override
    public CollectorPayloadBean put(final String key, final Serializable value) {
        if (null != value) {
            map.put(key, value);
        }
        return this;
    }

    @Override
    public Serializable get(final String key) {
        return map.get(key);
    }

    @Override
    public Map<String, Serializable> toMap() {
        return Map.copyOf(map);
    }

    public CollectorPayloadBean add(final CollectorPayloadBean other) {
        map.putAll(other.toMap());
        return this;
    }

}
