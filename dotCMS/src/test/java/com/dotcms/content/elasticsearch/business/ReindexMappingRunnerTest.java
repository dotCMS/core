package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * filesystem operation on one contentlet from wedging the reindex loop (issue #36498) without
 * permanently killing content indexing in the process (issue #37038).
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

    private static <T extends Exception> T expect(final Class<T> type, final Callable<?> call) {
        try {
            call.call();
            fail("expected " + type.getSimpleName());
            throw new AssertionError("unreachable");
        } catch (final Exception thrown) {
            if (!type.isInstance(thrown)) {
                throw new AssertionError(
                        "expected " + type.getSimpleName() + ", got " + thrown, thrown);
            }
            return type.cast(thrown);
        }
    }

    /** Times out one task, leaving it wedged and booked as abandoned. */
    private static void wedgeOneTask(final ReindexMappingRunner runner,
            final CountDownLatch release, final String description) {
        expect(ReindexMappingTimeoutException.class, () -> runner.run(() -> {
            wedge(release).run();
            return null;
        }, description));
    }

    // ── Timeout behavior ────────────────────────────────────────────────────

    /**
     * The core guarantee from issue #36498: a mapping that blocks past the timeout fails that
     * entry, and the next entry still processes — the loop is not wedged.
     */
    @Test
    public void blockingTaskTimesOutAndNextTaskStillRuns() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 4, 8, NO_CLEANUP);
        try {
            final ReindexMappingTimeoutException expected =
                    expect(ReindexMappingTimeoutException.class, () -> runner.run(() -> {
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
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, 8, NO_CLEANUP);
        assertEquals("mapped", runner.run(() -> {
            Thread.sleep(100);
            return "mapped";
        }, "fast entry"));
    }

    /** Null task results are legal (the production callable may return null). */
    @Test
    public void nullResultIsSupported() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, 8, NO_CLEANUP);
        assertNull(runner.run(() -> null, "void entry"));
    }

    /**
     * Async tasks run on a named <strong>platform</strong> worker. Not a virtual thread: file I/O
     * does not unmount one, so a hung stat would pin a carrier out of the JVM-wide pool of
     * {@code availableProcessors()} — which is what forced the permit accounting into the leaky
     * shape that killed indexing in issue #37038.
     */
    @Test
    public void asyncTaskRunsOnNamedPlatformThread() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, 8, NO_CLEANUP);
        final Thread worker = runner.run(Thread::currentThread, "thread probe");
        assertNotSame(Thread.currentThread(), worker);
        assertFalse("worker must be a platform thread", worker.isVirtual());
        assertTrue("worker name must identify the reindex mapping pool: " + worker.getName(),
                worker.getName().startsWith("dot-reindex-mapping-"));
    }

    // ── The issue #37038 regression: the cap must not be cumulative ──────────

    /**
     * <strong>The regression test for issue #37038.</strong> More timeouts than the concurrency cap
     * must not permanently disable the runner: a timeout returns its concurrency permit, because
     * the caller — not the task body — owns it. Before the fix, {@code cap + 1} timeouts over the
     * lifetime of the JVM killed content indexing until the node was restarted.
     */
    @Test
    public void moreTimeoutsThanTheConcurrencyCapDoesNotDisableTheRunner() throws Exception {
        final int cap = 2;
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(() -> 1, cap, 100, NO_CLEANUP);
        try {
            for (int i = 0; i < cap + 3; i++) {
                wedgeOneTask(runner, release, "wedged entry " + i);
            }
            // Nothing is holding a concurrency permit: they were all returned by the caller.
            assertEquals("healthy", runner.run(() -> "healthy", "healthy entry"));
            assertEquals(cap, runner.status().maxConcurrent());
            assertEquals("every timed-out task must still be counted as abandoned",
                    cap + 3, runner.status().abandoned());
            assertEquals(0, runner.status().inFlight());
        } finally {
            release.countDown();
        }
    }

    /** A merely-slow task interrupted by the timeout is not booked as abandoned at all. */
    @Test
    public void interruptibleSlowTaskIsFreedAndNotCountedAsAbandoned() throws Exception {
        final AtomicInteger cleanups = new AtomicInteger();
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(() -> 1, 1, 1, cleanups::incrementAndGet);
        expect(ReindexMappingTimeoutException.class, () -> runner.run(() -> {
            Thread.sleep(60_000); // interruptible — cancel(true) frees it
            return null;
        }, "slow interruptible entry"));

        final long deadline = System.currentTimeMillis() + 10_000;
        while (runner.status().abandoned() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }
        assertEquals("an interrupted task finishes, so it must not stay abandoned",
                0, runner.status().abandoned());
        assertFalse(runner.isDegraded());
        assertTrue("cleanup must have run for the cancelled task", cleanups.get() >= 1);
        assertEquals("ok", runner.run(() -> "ok", "next entry"));
    }

    // ── Circuit breaker on the abandonment ceiling ───────────────────────────

    /**
     * Reaching the abandonment ceiling means every task walked away from is still stuck: the
     * storage really is gone. Work is refused with a distinct, non-punitive exception so the
     * caller can back off instead of failing journal entries.
     */
    @Test
    public void abandonmentCeilingOpensTheCircuit() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 4, 2, NO_CLEANUP);
        try {
            assertFalse(runner.isDegraded());
            wedgeOneTask(runner, release, "wedged entry 1");
            assertFalse("one abandoned task of two must not open the circuit",
                    runner.isDegraded());
            wedgeOneTask(runner, release, "wedged entry 2");
            assertTrue(runner.isDegraded());

            final ReindexPoolExhaustedException exhausted =
                    expect(ReindexPoolExhaustedException.class,
                            () -> runner.run(() -> "never runs", "entry with a dead pool"));
            assertTrue(exhausted.isCircuitOpen());
            assertTrue(exhausted.getMessage().contains("pool exhausted"));
            assertTrue(exhausted.getMessage().contains("entry with a dead pool"));
        } finally {
            release.countDown();
        }
    }

    /**
     * The ceiling heals on its own: when the storage answers and the abandoned tasks return, the
     * circuit closes and indexing resumes — no restart, which is the whole point of the fix.
     */
    @Test
    public void circuitClosesWhenAbandonedTasksReturn() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger cleanups = new AtomicInteger();
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(() -> 1, 4, 1, cleanups::incrementAndGet);
        wedgeOneTask(runner, release, "wedged entry");
        assertTrue(runner.isDegraded());
        expect(ReindexPoolExhaustedException.class,
                () -> runner.run(() -> "rejected", "entry with a dead pool"));

        release.countDown(); // storage "recovers", the abandoned thread finishes

        final long deadline = System.currentTimeMillis() + 10_000;
        while (runner.isDegraded() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }
        assertFalse("the circuit must close on its own", runner.isDegraded());
        assertEquals(0, runner.status().abandoned());
        assertEquals("ok", runner.run(() -> "ok", "entry after recovery"));
        assertTrue("cleanup must have run when the wedged task finished", cleanups.get() >= 1);
    }

    // ── Concurrency cap ─────────────────────────────────────────────────────

    /** Up to maxConcurrent entries map at once; the cap only rejects the (N+1)th. */
    @Test
    public void tasksRunConcurrentlyUpToTheCap() throws Exception {
        final int cap = 3;
        final CountDownLatch allRunning = new CountDownLatch(cap);
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 30, cap, 8, NO_CLEANUP);
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
            assertEquals(cap, runner.status().inFlight());

            final ReindexPoolExhaustedException exhausted =
                    expect(ReindexPoolExhaustedException.class,
                            () -> runner.run(() -> "over cap", "extra entry"));
            assertTrue(exhausted.getMessage().contains("pool exhausted"));
            assertFalse("a busy pool is not a dead pool", exhausted.isCircuitOpen());

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
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 10, cap, 8, NO_CLEANUP);
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
        assertEquals("no permit leaked", 0, runner.status().inFlight());
        assertEquals(0, runner.status().abandoned());
    }

    // ── Exception propagation ───────────────────────────────────────────────

    /** The task's own runtime exception propagates so the journal entry gets its message. */
    @Test
    public void runtimeExceptionPropagates() {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, 8, NO_CLEANUP);
        final IllegalStateException thrown = expect(IllegalStateException.class,
                () -> runner.run(() -> {
                    throw new IllegalStateException("boom");
                }, "failing entry"));
        assertEquals("boom", thrown.getMessage());
    }

    /** Checked exceptions propagate unwrapped as well (mapping code throws DotDataException etc). */
    @Test
    public void checkedExceptionPropagatesUnwrapped() {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, 8, NO_CLEANUP);
        final IOException thrown = expect(IOException.class, () -> runner.run(() -> {
            throw new IOException("disk gone");
        }, "failing entry"));
        assertEquals("disk gone", thrown.getMessage());
    }

    /** Both rejection kinds stay DotRuntimeExceptions, so legacy callers keep working. */
    @Test
    public void guardExceptionsRemainDotRuntimeExceptions() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 1, 1, 1, NO_CLEANUP);
        try {
            final DotRuntimeException timedOut = expect(DotRuntimeException.class,
                    () -> runner.run(() -> {
                        wedge(release).run();
                        return null;
                    }, "wedged entry"));
            assertTrue(timedOut instanceof ReindexMappingTimeoutException);

            final DotRuntimeException exhausted = expect(DotRuntimeException.class,
                    () -> runner.run(() -> "rejected", "rejected entry"));
            assertTrue(exhausted instanceof ReindexPoolExhaustedException);
        } finally {
            release.countDown();
        }
    }

    // ── Disabled mode (timeout <= 0) ────────────────────────────────────────

    /** Timeout of 0 disables the guard entirely: the task runs inline on the calling thread. */
    @Test
    public void timeoutZeroRunsInlineOnCallerThread() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 0, 4, 8,
                () -> fail("cleanup must not run in inline mode"));
        assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
    }

    /** Negative timeouts behave like 0 (disabled), not like an instant timeout. */
    @Test
    public void negativeTimeoutAlsoRunsInline() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> -5, 4, 8, NO_CLEANUP);
        assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
    }

    /**
     * Inline mode is the documented off switch: it must work even with the circuit wide open, so
     * that setting the timeout to 0 restores the pre-guard behavior on a degraded instance.
     */
    @Test
    public void inlineModeBypassesAnOpenCircuit() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger timeout = new AtomicInteger(1);
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(timeout::get, 1, 1, NO_CLEANUP);
        try {
            wedgeOneTask(runner, release, "wedged entry");
            assertTrue(runner.isDegraded());
            timeout.set(0); // operator disables the guard while the pool is degraded
            assertSame(Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));
        } finally {
            release.countDown();
        }
    }

    /** The timeout is re-read per entry, so config changes apply without a restart. */
    @Test
    public void timeoutIsReadPerTask() throws Exception {
        final AtomicInteger timeout = new AtomicInteger(0);
        final ReindexMappingRunner runner =
                new ReindexMappingRunner(timeout::get, 4, 8, NO_CLEANUP);
        assertSame("timeout 0 must run inline",
                Thread.currentThread(), runner.run(Thread::currentThread, "inline entry"));

        timeout.set(1);
        final CountDownLatch release = new CountDownLatch(1);
        try {
            final ReindexMappingTimeoutException expected =
                    expect(ReindexMappingTimeoutException.class, () -> runner.run(() -> {
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
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 4, 8,
                cleanups::incrementAndGet);
        runner.run(() -> "ok", "entry one");
        expect(IllegalStateException.class, () -> runner.run(() -> {
            throw new IllegalStateException("boom");
        }, "entry two"));
        assertEquals(2, cleanups.get());
    }

    /** A failing cleanup must not corrupt the accounting and wedge the pool shut. */
    @Test
    public void failingCleanupDoesNotLeakPermits() throws Exception {
        final ReindexMappingRunner runner = new ReindexMappingRunner(() -> 5, 1, 1, () -> {
            throw new IllegalStateException("cleanup blew up");
        });
        for (int i = 0; i < 3; i++) {
            try {
                runner.run(() -> "ok", "entry " + i);
            } catch (final IllegalStateException fromCleanup) {
                // acceptable: cleanup failure may surface, but must not wedge the pool
            } catch (final ReindexPoolExhaustedException exhausted) {
                fail("permit leaked by throwing cleanup: " + exhausted.getMessage());
            }
        }
        assertEquals(0, runner.status().inFlight());
    }
}
