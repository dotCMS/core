package com.dotcms.analytics.track.collectors;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulate the basic signature for a collector payload bean
 * @author jsanca
 */
public interface CollectorPayloadBean {

    CollectorPayloadBean put(String key, Serializable value);
    Serializable get(String key);
    Map<String, Serializable> toMap();

    CollectorPayloadBean add(CollectorPayloadBean other);
}
