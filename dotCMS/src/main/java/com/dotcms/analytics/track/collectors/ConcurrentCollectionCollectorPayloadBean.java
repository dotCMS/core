package com.dotcms.analytics.track.collectors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Basic collection collector encapsulates a collection, in addition the CollectoPayloadBean is applied
 * to the first element of the collection, this is useful when you want to collect a set of data in a single
 * @author jsanca
 */
public class ConcurrentCollectionCollectorPayloadBean implements CollectionCollectorPayloadBean {

    private final List<CollectorPayloadBean> collection = new CopyOnWriteArrayList<>();

    public ConcurrentCollectionCollectorPayloadBean() {

    }

    public ConcurrentCollectionCollectorPayloadBean(final CollectorPayloadBean collectorPayloadBean) {
        collection.add(collectorPayloadBean);
    }

    @Override
    public List<CollectorPayloadBean> getCollection() {
        return collection;
    }

    public CollectorPayloadBean first () {

        if (collection.isEmpty()) {
            collection.add(new ConcurrentCollectorPayloadBean());
        }

        return collection.get(0);
    }

    @Override
    public CollectionCollectorPayloadBean add(final CollectorPayloadBean collectorPayloadBean) {

        collection.add(collectorPayloadBean);
        return this;
    }
}
