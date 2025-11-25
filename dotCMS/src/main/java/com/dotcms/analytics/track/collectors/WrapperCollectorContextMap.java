package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;

import java.util.Map;

/**
 * Represent a Wrapper of a {@link CollectorContextMap}, it allows override some of
 * the attribute from the original {@link CollectorContextMap}
 */
public class WrapperCollectorContextMap implements CollectorContextMap {
    private final CollectorContextMap collectorContextMap;

    private final  Map<String, Object> toOverride;

    public WrapperCollectorContextMap(final CollectorContextMap collectorContextMap,
                                      final  Map<String, Object> toOverride){

        this.collectorContextMap = collectorContextMap;
        this.toOverride = toOverride;
    }

    @Override
    public Object get(String key) {

        return toOverride.containsKey(key) ? toOverride.get(key) : collectorContextMap.get(key);
    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return this.collectorContextMap.getRequestMatcher();
    }
}
