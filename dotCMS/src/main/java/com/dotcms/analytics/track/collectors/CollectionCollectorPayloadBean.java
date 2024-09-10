package com.dotcms.analytics.track.collectors;

import java.util.List;

/**
 * Is a collection of payloads
 * @author jsanca
 */
public interface CollectionCollectorPayloadBean  {

    List<CollectorPayloadBean> getCollection();
    CollectionCollectorPayloadBean add(CollectorPayloadBean collectorPayloadBean);
    CollectorPayloadBean first ();

}
