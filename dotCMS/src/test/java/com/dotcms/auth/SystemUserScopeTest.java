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

    /**
     * A task wrapped while the scope is active runs inside the scope on the thread that executes it, and
     * leaves nothing behind there once it ends.
     */
    @Test
    public void test_wrapped_runnable_carries_the_scope_to_another_thread() throws Exception {
        final AtomicBoolean activeInside = new AtomicBoolean(false);
        final Runnable task;
        try (SystemUserScope ignored = SystemUserScope.open()) {
            task = SystemUserScope.wrap(() -> activeInside.set(SystemUserScope.isActive()));
        }
        final AtomicBoolean activeAfter = new AtomicBoolean(true);
        final Thread worker = new Thread(() -> {
            task.run();
            activeAfter.set(SystemUserScope.isActive());
        });
        worker.start();
        worker.join();
        assertTrue("scope was not carried to the executing thread", activeInside.get());
        assertFalse("scope leaked on the executing thread", activeAfter.get());
    }

    /**
     * A task wrapped when no scope is active does not open one, even if it runs while the dispatching
     * thread is inside a scope.
     */
    @Test
    public void test_wrapped_task_without_a_scope_does_not_open_one() throws Exception {
        final AtomicBoolean activeInside = new AtomicBoolean(true);
        final Runnable task = SystemUserScope.wrap(() -> activeInside.set(SystemUserScope.isActive()));
        try (SystemUserScope ignored = SystemUserScope.open()) {
            final Thread worker = new Thread(task);
            worker.start();
            worker.join();
        }
        assertFalse(activeInside.get());
    }

    /**
     * The Callable variant carries the scope, returns the value and propagates exceptions, closing the scope
     * on the way out.
     */
    @Test
    public void test_wrapped_callable_carries_the_scope_returns_and_propagates() throws Exception {
        final java.util.concurrent.Callable<Boolean> ok;
        final java.util.concurrent.Callable<Boolean> failing;
        try (SystemUserScope ignored = SystemUserScope.open()) {
            ok = SystemUserScope.wrap(SystemUserScope::isActive);
            failing = SystemUserScope.wrap(() -> {
                throw new IllegalStateException("boom");
            });
        }
        final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            assertTrue(pool.submit(ok).get());
            try {
                pool.submit(failing).get();
                fail("exception was swallowed");
            } catch (final java.util.concurrent.ExecutionException expected) {
                assertTrue(expected.getCause() instanceof IllegalStateException);
            }
            assertFalse("scope leaked on the pool thread", pool.submit(SystemUserScope::isActive).get());
        } finally {
            pool.shutdownNow();
        }
    }
}
