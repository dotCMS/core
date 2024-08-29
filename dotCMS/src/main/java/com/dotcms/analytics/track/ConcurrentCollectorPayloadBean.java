package com.dotcms.analytics.track;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a collector payload bean that is thread safe
 * @author jsanca
 */
public class ConcurrentCollectorPayloadBean implements CollectorPayloadBean {

    private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<>();

    @Override
    public CollectorPayloadBean put(final String key, final Serializable value) {
        map.put(key, value);
        return this;
    }

    @Override
    public Serializable get(final String key) {
        return map.get(key);
    }

}
