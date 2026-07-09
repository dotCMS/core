package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.exception.DotRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Unit tests for {@link ReindexMappingRunner} — the bounded-timeout guard that keeps a hung
 * filesystem operation on one contentlet from wedging the reindex loop (issue #36498).
 */
public class ReindexMappingRunnerTest {

    private static final Runnable NO_CLEANUP = () -> {};

    /** Blocks "uninterruptibly" like a native stat: ignores interrupts until released. */
    private static Runnable wedge(final CountDownLatch release) {
        return () -> {
            while (true) {
                try {
                    if (release.await(10, TimeUnit.SECONDS)) {
                        return;
                    }
                } catch (final InterruptedException ignored) {
                    // a thread wedged in native I/O does not respond to interrupt
                }
            }
        };
    }

    private static DotRuntimeException expectDotRuntime(final Callable<?> call) {
        try {
            call.call();
            fail("expected DotRuntimeException");
            throw new AssertionError("unreachable");
        } catch (final DotRuntimeException expected) {
            return expected;
        } catch (final Exception other) {
            throw new AssertionError("expected DotRuntimeException, got " + other, other);
        }
    }

    /** Polls until the runner accepts and completes a trivial task, proving a permit is free. */
    private static void awaitFreePermit(final ReindexMappingRunner runner) throws Exception {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            try {
                assertEquals("ok", runner.run(() -> "ok", "probe"));
                return;
            } catch (final DotRuntimeException stillExhausted) {
                if (System.currentTimeMillis() > deadline) {
                    throw stillExhausted;
                }
                Thread.sleep(50);
            }
        }
    }

    // ── Timeout behavior ────────────────────────────────────────────────────

    /**
     * The core guarantee from issue #36498: a mapping that blocks past the timeout fails that
     * entry, and the next entry still processes — the loop is not wedged.
     */
    @Test
    public void blockingTaskTimesOutAndNextTaskStillRuns() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 4, NO_CLEANUP);
        try {
            final DotRuntimeException expected = expectDotRuntime(() ->
                    runner.run(() -> {
                        wedge(release).run();
                        return null;
                    }, "wedged entry"));
            assertTrue(expected.getMessage().contains("Timed out after 1s"));
            assertTrue(expected.getMessage().contains("wedged entry"));
            // The wedged thread is abandoned; the next entry maps normally on a fresh thread.
            assertEquals("ok", runner.run(() -> "ok", "next entry"));
        } finally {
            release.countDown();
        }
    }

    /** A task that finishes within the timeout returns its value — no false positives. */
    @Test
    public void fastTaskCompletesWithinTimeout() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, NO_CLEANUP);
        assertEquals("mapped", runner.run(() -> {
            Thread.sleep(100);
            return "mapped";
        }, "fast entry"));
    }

    /** Null task results are legal (the production callable returns null). */
    @Test
    public void nullResultIsSupported() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, NO_CLEANUP);
        assertNull(runner.run(() -> null, "void entry"));
    }

    /** Async tasks run on a named virtual worker thread, not the caller thread. */
    @Test
    public void asyncTaskRunsOnNamedVirtualThread() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, NO_CLEANUP);
        final Thread worker = runner.run(Thread::currentThread, "thread probe");
        assertNotSame(Thread.currentThread(), worker);
        assertTrue("worker must be a virtual thread", worker.isVirtual());
        assertTrue("worker name must identify the reindex mapping pool: " + worker.getName(),
                worker.getName().startsWith("dot-reindex-mapping-"));
    }

    /**
     * Timeout cancellation interrupts the worker, so a merely-slow task blocked in an
     * interruptible wait is freed immediately (only native-wedged threads are truly abandoned)
     * and its permit is recovered.
     */
    @Test
    public void interruptibleSlowTaskIsFreedByCancellationAndPermitRecovered() throws Exception {
        final AtomicInteger cleanups = new AtomicInteger();
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(() -> 1, 1, cleanups::incrementAndGet);
        final DotRuntimeException expected = expectDotRuntime(() ->
                runner.run(() -> {
                    Thread.sleep(60_000); // interruptible — cancel(true) frees it
                    return null;
                }, "slow interruptible entry"));
        assertTrue(expected.getMessage().contains("Timed out"));
        // maxThreads is 1: this only succeeds if the interrupted task released its permit.
        awaitFreePermit(runner);
        assertTrue("cleanup must have run for the cancelled task", cleanups.get() >= 1);
    }

    // ── Exception propagation ───────────────────────────────────────────────

    /** The task's own runtime exception propagates so the journal entry gets its message. */
    @Test
    public void runtimeExceptionPropagates() {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, NO_CLEANUP);
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

    /** Checked exceptions propagate unwrapped as well (mapping code throws DotDataException etc). */
    @Test
    public void checkedExceptionPropagatesUnwrapped() {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, NO_CLEANUP);
        try {
            runner.run(() -> {
                throw new IOException("disk gone");
            }, "failing entry");
            fail("expected task exception");
        } catch (final Exception e) {
            assertTrue("expected IOException, got " + e, e instanceof IOException);
            assertEquals("disk gone", e.getMessage());
        }
    }

    // ── Pool bounding / permit accounting ───────────────────────────────────

    /** When every in-flight slot is wedged, new work is rejected loudly instead of queueing. */
    @Test
    public void exhaustedPoolRejectsWithClearError() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 1, NO_CLEANUP);
        try {
            final DotRuntimeException timedOut = expectDotRuntime(() ->
                    runner.run(() -> {
                        wedge(release).run();
                        return null;
                    }, "first wedged entry"));
            assertTrue(timedOut.getMessage().contains("Timed out"));

            final DotRuntimeException exhausted = expectDotRuntime(() ->
                    runner.run(() -> "never runs", "entry with no free thread"));
            assertTrue(exhausted.getMessage().contains("pool exhausted"));
            assertTrue(exhausted.getMessage().contains("entry with no free thread"));
        } finally {
            release.countDown();
        }
    }

    /**
     * A wedged task that eventually returns (storage recovers) gives its permit back: the pool
     * heals instead of staying permanently exhausted.
     */
    @Test
    public void permitIsRecoveredWhenWedgedTaskEventuallyFinishes() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger cleanups = new AtomicInteger();
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(() -> 1, 1, cleanups::incrementAndGet);
        expectDotRuntime(() -> runner.run(() -> {
            wedge(release).run();
            return null;
        }, "wedged entry"));
        expectDotRuntime(() -> runner.run(() -> "rejected", "exhausted entry"));

        release.countDown(); // storage "recovers", the abandoned thread finishes
        awaitFreePermit(runner);
        assertTrue("cleanup must have run when the wedged task finished", cleanups.get() >= 1);
    }

    /** Up to maxThreads entries map concurrently; the cap only rejects the (N+1)th. */
    @Test
    public void tasksRunConcurrentlyUpToTheCap() throws Exception {
        final int cap = 3;
        final CountDownLatch allRunning = new CountDownLatch(cap);
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 30, cap, NO_CLEANUP);
        final ExecutorService callers = Executors.newFixedThreadPool(cap);
        try {
            final List<Future<String>> inFlight = new ArrayList<>();
            for (int i = 0; i < cap; i++) {
                inFlight.add(callers.submit(() -> runner.run(() -> {
                    allRunning.countDown();
                    release.await(30, TimeUnit.SECONDS);
                    return "done";
                }, "concurrent entry")));
            }
            assertTrue("all " + cap + " tasks must be running concurrently",
                    allRunning.await(10, TimeUnit.SECONDS));

            final DotRuntimeException exhausted = expectDotRuntime(() ->
                    runner.run(() -> "over cap", "extra entry"));
            assertTrue(exhausted.getMessage().contains("pool exhausted"));

            release.countDown();
            for (final Future<String> result : inFlight) {
                assertEquals("done", result.get(10, TimeUnit.SECONDS));
            }
        } finally {
            release.countDown();
            callers.shutdownNow();
        }
    }

    /** Hammering the runner from many callers loses no results and leaks no permits. */
    @Test
    public void parallelCallersAllSucceedAndPermitsAreNotLeaked() throws Exception {
        final int cap = 8;
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 10, cap, NO_CLEANUP);
        final ExecutorService callers = Executors.newFixedThreadPool(cap);
        try {
            final List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int value = i;
                results.add(callers.submit(() -> runner.run(() -> value, "entry " + value)));
            }
            int sum = 0;
            for (final Future<Integer> result : results) {
                sum += result.get(30, TimeUnit.SECONDS);
            }
            assertEquals(4950, sum);
        } finally {
            callers.shutdownNow();
        }
        // No permit leaked: the full cap is still available for concurrent work.
        for (int i = 0; i < cap; i++) {
            awaitFreePermit(runner);
        }
    }

    // ── Disabled mode (timeout <= 0) ────────────────────────────────────────

    /** Timeout of 0 disables the guard entirely: the task runs inline on the calling thread. */
    @Test
    public void timeoutZeroRunsInlineOnCallerThread() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 0, 4,
                () -> fail("cleanup must not run in inline mode"));
        assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
    }

    /** Negative timeouts behave like 0 (disabled), not like an instant timeout. */
    @Test
    public void negativeTimeoutAlsoRunsInline() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> -5, 4, NO_CLEANUP);
        assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
    }

    /** Inline mode does not consume permits — it works even when the async pool is exhausted. */
    @Test
    public void inlineModeBypassesTheExhaustedPool() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger timeout = new AtomicInteger(1);
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(timeout::get, 1, NO_CLEANUP);
        try {
            expectDotRuntime(() -> runner.run(() -> {
                wedge(release).run();
                return null;
            }, "wedged entry"));
            timeout.set(0); // operator disables the guard while the pool is wedged
            assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
        } finally {
            release.countDown();
        }
    }

    /** The timeout is re-read per entry, so config changes apply without a restart. */
    @Test
    public void timeoutIsReadPerTask() throws Exception {
        final AtomicInteger timeout = new AtomicInteger(0);
        final ReindexMappingRunner runner = new ReindexMappingRunner(timeout::get, 4, NO_CLEANUP);
        assertSame("timeout 0 must run inline",
                Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));

        timeout.set(1);
        final CountDownLatch release = new CountDownLatch(1);
        try {
            final DotRuntimeException expected = expectDotRuntime(() ->
                    runner.run(() -> {
                        wedge(release).run();
                        return null;
                    }, "wedged entry"));
            assertTrue(expected.getMessage().contains("Timed out after 1s"));
        } finally {
            release.countDown();
        }
    }

    // ── Cleanup contract ────────────────────────────────────────────────────

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

    /** A failing cleanup must not leak the permit and wedge the pool shut. */
    @Test
    public void failingCleanupDoesNotLeakPermits() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 1, () -> {
            throw new IllegalStateException("cleanup blew up");
        });
        // With maxThreads 1, a single leaked permit would make every later call reject
        // with "pool exhausted".
        for (int i = 0; i < 3; i++) {
            try {
                runner.run(() -> "ok", "entry " + i);
            } catch (final IllegalStateException fromCleanup) {
                // acceptable: cleanup failure may surface, but must not wedge the pool
            } catch (final DotRuntimeException exhausted) {
                fail("permit leaked by throwing cleanup: " + exhausted.getMessage());
            }
        }
    }
}
