package com.dotcms.saml.service.impl;

import com.dotcms.saml.service.internal.AssertionReplayStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test double for {@link AssertionReplayStore}: same contract as the database store, in memory.
 */
public class InMemoryAssertionReplayStore implements AssertionReplayStore {

    private final Map<String, Long> used = new ConcurrentHashMap<>();

    @Override
    public boolean markUsed(final String key, final long expiresAt) {

        final long now = System.currentTimeMillis();
        this.used.values().removeIf(expiry -> expiry < now);
        return null == this.used.putIfAbsent(key, expiresAt);
    }
}
