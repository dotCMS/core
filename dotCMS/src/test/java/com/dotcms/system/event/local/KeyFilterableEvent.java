package com.dotcms.system.event.local;

import com.dotcms.system.event.local.model.KeyFilterable;

/**
 * This implementation of KeyFilterable is intended for testing purposes
 */
public class KeyFilterableEvent implements KeyFilterable {

    private final String key;

    public KeyFilterableEvent(final String key) {
        this.key = key;
    }

    @Override
    public Comparable getKey() {
        return key;
    }
}
