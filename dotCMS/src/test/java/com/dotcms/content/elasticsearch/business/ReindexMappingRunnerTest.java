package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.exception.DotRuntimeException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Unit tests for {@link ReindexMappingRunner} — the bounded-timeout guard that keeps a hung
 * filesystem operation on one contentlet from wedging the reindex loop (issue #36498).
 */
public class ReindexMappingRunnerTest {

    /** Blocks "uninterruptibly" like a native stat: ignores interrupts until released. */
    private static Runnable wedge(final CountDownLatch release) {
        return () -> {
            while (true) {
                try {
                    if (release.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        return;
                    }
                } catch (final InterruptedException ignored) {
                    // a thread wedged in native I/O does not respond to interrupt
                }
            }
        };
    }

    /**
     * A mapping that blocks past the timeout fails that entry, and the next entry still
     * processes — the loop is not wedged.
     */
    @Test
    public void blockingTaskTimesOutAndNextTaskStillRuns() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 4, () -> {});
        try {
            runner.run(() -> {
                wedge(release).run();
                return null;
            }, "wedged entry");
            fail("expected timeout");
        } catch (final DotRuntimeException expected) {
            assertTrue(expected.getMessage().contains("Timed out after 1s"));
            assertTrue(expected.getMessage().contains("wedged entry"));
        }
        // The wedged thread is abandoned; the next entry maps normally on a fresh thread.
        assertEquals("ok", runner.run(() -> "ok", "next entry"));
        release.countDown();
    }

    /** Timeout of 0 disables the guard entirely: the task runs inline on the calling thread. */
    @Test
    public void timeoutZeroRunsInlineOnCallerThread() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 0, 4,
                () -> fail("cleanup must not run in inline mode"));
        assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
    }

    /** The task's own exception propagates so the journal entry is failed with its message. */
    @Test
    public void taskExceptionPropagates() {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, () -> {});
        try {
            runner.run(() -> {
                throw new IllegalStateException("boom");
            }, "failing entry");
            fail("expected task exception");
        } catch (final Exception e) {
            assertTrue(e instanceof IllegalStateException);
            assertEquals("boom", e.getMessage());
        }
    }

    /** When every worker thread is wedged, new work is rejected loudly instead of queueing. */
    @Test
    public void exhaustedPoolRejectsWithClearError() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 1, () -> {});
        try {
            runner.run(() -> {
                wedge(release).run();
                return null;
            }, "first wedged entry");
            fail("expected timeout");
        } catch (final DotRuntimeException expected) {
            assertTrue(expected.getMessage().contains("Timed out"));
        }
        try {
            runner.run(() -> "never runs", "entry with no free thread");
            fail("expected pool exhaustion");
        } catch (final DotRuntimeException expected) {
            assertTrue(expected.getMessage().contains("pool exhausted"));
        }
        release.countDown();
    }

    /** Per-task cleanup runs on the worker thread after successful and failed tasks. */
    @Test
    public void cleanupRunsAfterEachAsyncTask() throws Exception {
        final AtomicInteger cleanups = new AtomicInteger();
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4,
                cleanups::incrementAndGet);
        runner.run(() -> "ok", "entry one");
        try {
            runner.run(() -> {
                throw new IllegalStateException("boom");
            }, "entry two");
            fail("expected task exception");
        } catch (final IllegalStateException ignored) {
            // expected
        }
        assertEquals(2, cleanups.get());
    }
}
