package com.dotcms.analytics.track.collectors;

import java.io.Serializable;
import java.util.Map;

/**
 * This class provides the default implementation for the {@link CollectorPayloadBean}
 * interface.
 * Concurrent Collector Payload Bean, but using a base map with is the source fo truth,
 * means that the map is the one that is going to be used and can not be overriden by any collector
 *
 * @author jsanca
 */
public class ConcurrentCollectorPayloadBeanWithBaseMap extends ConcurrentCollectorPayloadBean {

    private final Map<String, Serializable> baseMap;

    public ConcurrentCollectorPayloadBeanWithBaseMap(final Map<String, Serializable> customMap) {
        super(customMap);
        this.baseMap = customMap;
    }

    @Override
    public CollectorPayloadBean put(final String key, final Serializable value) {

        if (baseMap.containsKey(key)) {
            return this;
        }

        super.put(key, value);
        return this;
    }


    public CollectorPayloadBean add(final CollectorPayloadBean other) {

        other.toMap().entrySet().stream()
                .filter(entry -> !baseMap.containsKey(entry.getKey()))
                .forEach(entry -> super.put(entry.getKey(), entry.getValue()));
        return this;
    }
}
