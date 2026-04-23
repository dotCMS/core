package com.dotcms.auth.dotAuth.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Expiry-check semantics for the lightweight session record. */
class DotAuthSessionTest {

    @Test
    void expiresAtInFuture_isNotExpired() {
        final long now = System.currentTimeMillis();
        final DotAuthSession session = new DotAuthSession("user-123", now, now + 60_000L);
        assertFalse(session.isExpired(), "session expiring 60s from now must not be expired");
    }

    @Test
    void expiresAtInPast_isExpired() {
        final long now = System.currentTimeMillis();
        final DotAuthSession session = new DotAuthSession("user-123", now - 120_000L, now - 60_000L);
        assertTrue(session.isExpired(), "session expiring 60s ago must be expired");
    }

    @Test
    void accessors_roundTripValues() {
        final DotAuthSession session = new DotAuthSession("user-42", 1_000L, 2_000L);
        assertEquals("user-42", session.getUserId());
        assertEquals(1_000L, session.getCreatedAt());
        assertEquals(2_000L, session.getExpiresAt());
    }
}
