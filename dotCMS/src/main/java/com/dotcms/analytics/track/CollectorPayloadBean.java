package com.dotcms.analytics.track;

import java.io.Serializable;

/**
 * Encapsulate the basic signature for a collector payload bean
 * @author jsanca
 */
public interface CollectorPayloadBean {

    CollectorPayloadBean put(String key, Serializable value);
    Serializable get(String key);
}
