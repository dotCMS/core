package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.business.Cachable;

import java.util.Map;

public interface CustomAttributeCache extends Cachable {

    Map<String, String> get(String eventTypeName);
    void put (String eventTypeName, Map<String, String> attributesMatch);

}
