package com.dotcms.system.event.local;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;

public abstract class KeyFilterableTestSubscriberTest implements EventSubscriber<KeyFilterableEvent> , KeyFilterable {

    private final String key;

    public KeyFilterableTestSubscriberTest(final String key) {
        this.key = key;
    }

    @Override
    public Comparable getKey() {
        return key;
    }


}
