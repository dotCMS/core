package com.dotcms.concurrent;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.SystemUserScope;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that work handed to a {@link DotSubmitter} from inside a {@link SystemUserScope} runs inside that
 * scope on the pool thread, and that work handed over outside a scope does not.
 */
public class DotConcurrentFactorySystemUserScopeTest extends UnitTestBase {

    private static final long WAIT_SECONDS = 10;

    private static DotSubmitter pool() {
        return DotConcurrentFactory.getInstance().getSubmitter("system-user-scope-test-pool");
    }

    private static DotSubmitter single() {
        return DotConcurrentFactory.getInstance().getSingleSubmitter("system-user-scope-test-single");
    }

    /** Hands a probe to the submitter in every way it offers and reports whether each ran inside a scope. */
    private static boolean[] probe(final DotSubmitter submitter, final boolean insideScope) throws Exception {
        final boolean[] seen = new boolean[5];
        final CountDownLatch done = new CountDownLatch(3);
        final AtomicBoolean viaExecute = new AtomicBoolean();
        final AtomicBoolean viaDelay = new AtomicBoolean();
        final AtomicBoolean viaSchedule = new AtomicBoolean();

        final Runnable handOver = () -> {
            try {
                seen[0] = submitter.submit(SystemUserScope::isActive).get(WAIT_SECONDS, TimeUnit.SECONDS);
                seen[1] = (Boolean) submitter.submit((Runnable) () -> { }, 1, TimeUnit.MILLISECONDS)
                        .get(WAIT_SECONDS, TimeUnit.SECONDS) == null
                        && submitter.submit(SystemUserScope::isActive, 1, TimeUnit.MILLISECONDS)
                        .get(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
            submitter.execute(() -> {
                viaExecute.set(SystemUserScope.isActive());
                done.countDown();
            });
            submitter.delay(() -> {
                viaDelay.set(SystemUserScope.isActive());
                done.countDown();
            }, 1, TimeUnit.MILLISECONDS);
            submitter.schedule(() -> {
                viaSchedule.set(SystemUserScope.isActive());
                done.countDown();
            }, 1, TimeUnit.MILLISECONDS);
        };

        if (insideScope) {
            try (SystemUserScope ignored = SystemUserScope.open()) {
                handOver.run();
            }
        } else {
            handOver.run();
        }
        assertTrue("the hand-off did not finish", done.await(WAIT_SECONDS, TimeUnit.SECONDS));
        seen[2] = viaExecute.get();
        seen[3] = viaDelay.get();
        seen[4] = viaSchedule.get();
        return seen;
    }

    private static void assertAll(final boolean[] seen, final boolean expected, final String where) {
        final String[] names = {"submit(Callable)", "submit(Callable, delay)", "execute", "delay", "schedule"};
        for (int i = 0; i < seen.length; i++) {
            if (expected) {
                assertTrue(where + ": " + names[i] + " did not carry the scope", seen[i]);
            } else {
                assertFalse(where + ": " + names[i] + " opened a scope nobody asked for", seen[i]);
            }
        }
    }

    /** Pool submitter: every hand-off made inside a scope runs as the system user. */
    @Test
    public void test_pool_submitter_carries_the_scope() throws Exception {
        assertAll(probe(pool(), true), true, "pool");
    }

    /** Single-thread submitter: every hand-off made inside a scope runs as the system user. */
    @Test
    public void test_single_submitter_carries_the_scope() throws Exception {
        assertAll(probe(single(), true), true, "single");
    }

    /** Hand-offs made outside a scope do not run inside one. */
    @Test
    public void test_no_scope_is_opened_when_none_was_active() throws Exception {
        assertAll(probe(pool(), false), false, "pool");
        assertAll(probe(single(), false), false, "single");
    }
}
