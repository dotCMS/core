package com.dotcms.auth;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link SystemUserScope}: a thread-bound, server-side scope during which identity resolution
 * answers with the built-in account. Nothing a request or a session carries can open it.
 */
public class SystemUserScopeTest {

    /**
     * Not active by default; active inside the scope; gone after it closes.
     */
    @Test
    public void test_active_inside_and_gone_after_close() {
        assertFalse(SystemUserScope.isActive());
        try (SystemUserScope ignored = SystemUserScope.open()) {
            assertTrue(SystemUserScope.isActive());
        }
        assertFalse(SystemUserScope.isActive());
    }

    /**
     * An exception inside the scope still closes it.
     */
    @Test
    public void test_scope_closes_when_an_exception_is_thrown() {
        try {
            try (SystemUserScope ignored = SystemUserScope.open()) {
                if (SystemUserScope.isActive()) {
                    throw new IllegalStateException("boom");
                }
                fail("scope was not active inside the try block");
            }
        } catch (final IllegalStateException expected) {
            assertFalse(SystemUserScope.isActive());
        }
    }

    /**
     * Another thread does not see this thread's scope.
     */
    @Test
    public void test_scope_is_not_active_on_another_thread() throws Exception {
        final AtomicBoolean seenElsewhere = new AtomicBoolean(true);
        try (SystemUserScope ignored = SystemUserScope.open()) {
            final Thread other = new Thread(() -> seenElsewhere.set(SystemUserScope.isActive()));
            other.start();
            other.join();
        }
        assertFalse(seenElsewhere.get());
    }

    /**
     * Nested scopes keep it active until the outermost one closes.
     */
    @Test
    public void test_nested_scopes_stay_active_until_the_outermost_closes() {
        try (SystemUserScope outer = SystemUserScope.open()) {
            try (SystemUserScope inner = SystemUserScope.open()) {
                assertTrue(SystemUserScope.isActive());
            }
            assertTrue(SystemUserScope.isActive());
        }
        assertFalse(SystemUserScope.isActive());
    }
}
