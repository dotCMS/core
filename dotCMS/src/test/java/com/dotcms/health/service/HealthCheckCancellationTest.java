package com.dotcms.health.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Documents why {@code runAllHealthChecksAndWait} moved from {@code CompletableFuture.allOf(...)
 * .get(timeout)} to a {@link StructuredTaskScope}.
 *
 * <p>Both shapes stop waiting after the timeout. Only one of them stops the work. The old shape
 * left a hung check running on {@code HealthCheckConfig.THREAD_POOL_SIZE}'s bounded pool with
 * nothing reporting it, which is the same failure mode as issue #37038 in a different subsystem:
 * abandoning something that keeps consuming a shared, bounded resource.</p>
 *
 * <p>These tests exercise the concurrency shapes directly rather than the manager, so they need no
 * registered checks and no container.</p>
 */
public class HealthCheckCancellationTest {

    /** How long a wedged check pretends to take — far longer than any timeout used here. */
    private static final long WEDGED_MS = 10_000L;

    private static final long TIMEOUT_MS = 300L;

    /** Counts tasks that have started and not yet returned. */
    private final AtomicInteger inFlight = new AtomicInteger();

    private void wedgedCheck() {
        inFlight.incrementAndGet();
        try {
            Thread.sleep(WEDGED_MS);
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            inFlight.decrementAndGet();
        }
    }

    /**
     * Method to test: the {@code CompletableFuture.allOf(...).get(timeout)} shape this replaced.
     * Given scenario: four wedged checks and a timeout far shorter than they take.
     * Expected result: the timeout fires and every task is STILL RUNNING. get() stops waiting; it
     * does not cancel. This is the behaviour the change removes.
     */
    @Test
    public void test_allOfWithTimeout_doesNotCancelAnything() throws Exception {

        final ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                futures.add(CompletableFuture.runAsync(this::wedgedCheck, pool));
            }

            assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            Thread.sleep(200);
            assertEquals("the timeout abandoned them; they are still holding pool threads",
                    4, inFlight.get());
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Method to test: the {@link StructuredTaskScope} shape now in use.
     * Given scenario: the same four wedged checks and the same timeout.
     * Expected result: the timeout fires and, by the time the block has exited, nothing is running.
     * close() cancels the subtasks and waits for them, so no work escapes the method.
     */
    @Test
    public void test_scopeWithTimeout_cancelsAndWaits() {

        assertThrows(StructuredTaskScope.TimeoutException.class, () -> {
            try (var scope = StructuredTaskScope.open(Joiner.<Void>awaitAll(),
                    cfg -> cfg.withTimeout(Duration.ofMillis(TIMEOUT_MS)))) {

                for (int i = 0; i < 4; i++) {
                    scope.fork(() -> {
                        wedgedCheck();
                        return null;
                    });
                }
                scope.join();
            }
        });

        assertEquals("close() cancelled them and did not return until they were done",
                0, inFlight.get());
    }

    /**
     * Method to test: the {@link Joiner#awaitAll()} choice.
     * Given scenario: one check throws while the others succeed.
     * Expected result: the failure does not cancel its siblings and does not propagate. This
     * preserves the previous contract — carry on with whatever results arrived — which
     * {@code awaitAllSuccessfulOrThrow()} would have changed.
     */
    @Test
    public void test_awaitAll_oneFailingCheckDoesNotCancelTheRest() throws Exception {

        final AtomicInteger completed = new AtomicInteger();

        try (var scope = StructuredTaskScope.open(Joiner.<Void>awaitAll(),
                cfg -> cfg.withTimeout(Duration.ofSeconds(5)))) {

            scope.fork(() -> {
                throw new IllegalStateException("one check blew up");
            });
            for (int i = 0; i < 3; i++) {
                scope.fork(() -> {
                    Thread.sleep(50);
                    completed.incrementAndGet();
                    return null;
                });
            }
            scope.join();
        }

        assertEquals("the three healthy checks still ran to completion", 3, completed.get());
    }

    /**
     * Method to test: {@link StructuredTaskScope#fork(java.util.concurrent.Callable)}.
     * Given scenario: any subtask.
     * Expected result: it runs on a virtual thread. Health checks block on sockets — databases,
     * search endpoints, HTTP probes — which is the case virtual threads exist for.
     */
    @Test
    public void test_subtasksRunOnVirtualThreads() throws Exception {

        final List<Boolean> virtual = java.util.Collections.synchronizedList(new ArrayList<>());

        try (var scope = StructuredTaskScope.open(Joiner.<Void>awaitAll())) {
            for (int i = 0; i < 4; i++) {
                scope.fork(() -> {
                    virtual.add(Thread.currentThread().isVirtual());
                    return null;
                });
            }
            scope.join();
        }

        assertEquals(4, virtual.size());
        assertTrue("every subtask ran on a virtual thread", virtual.stream().allMatch(v -> v));
    }
}
