package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * Covers {@link BrowserAPIImpl#inParallelChunks(List, int, int, java.util.function.Function)} — the
 * two-gatherer pipeline that replaced a hand-built {@code CompletableFuture} array in
 * {@code loadContentletsParallel} and a future list in {@code hydrateContentletsInParallel}.
 *
 * <p>The old shape produced ordered results as a side effect of iterating the futures in creation
 * order, and one of the two sites said so in a comment. These tests make that a checked property
 * rather than a remark, and pin the concurrency bound, which is the parameter that matters when the
 * loader is holding a database connection.</p>
 */
public class BrowserAPIImplParallelChunksTest {

    /**
     * Method to test: {@link BrowserAPIImpl#inParallelChunks(List, int, int, java.util.function.Function)}
     * Given scenario: a chunk size that does not divide the input evenly.
     * Expected result: every chunk is seen, the last one short, and nothing is dropped.
     */
    @Test
    public void test_inParallelChunks_chunksCoverEveryElement_lastOneShort() {

        final List<Integer> chunkSizes = Collections.synchronizedList(new ArrayList<>());

        final List<Integer> out = BrowserAPIImpl.inParallelChunks(
                IntStream.rangeClosed(1, 25).boxed().toList(), 10, 4,
                chunk -> {
                    chunkSizes.add(chunk.size());
                    return chunk;
                });

        assertEquals(25, out.size());
        assertEquals("three chunks: 10, 10 and the short tail", 3, chunkSizes.size());
        assertEquals(5, (int) chunkSizes.stream().sorted().findFirst().orElseThrow());
    }

    /**
     * Method to test: {@link BrowserAPIImpl#inParallelChunks(List, int, int, java.util.function.Function)}
     * Given scenario: loaders that finish in a deliberately reversed order — the first chunk sleeps
     * longest, so a shape that returned results as they completed would come back scrambled.
     * Expected result: input order regardless. This is the property the old code got from iterating
     * the futures in creation order, and the one {@code parallelStream()} does not promise.
     */
    @Test
    public void test_inParallelChunks_preservesInputOrder_whateverTheCompletionOrder() {

        final AtomicInteger seen = new AtomicInteger();

        final List<Integer> out = BrowserAPIImpl.inParallelChunks(
                IntStream.rangeClosed(1, 20).boxed().toList(), 5, 4,
                chunk -> {
                    // The earlier the chunk, the longer it takes to come back.
                    final int order = seen.getAndIncrement();
                    try {
                        Thread.sleep(60L - (order * 15L));
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return chunk;
                });

        assertEquals(IntStream.rangeClosed(1, 20).boxed().toList(), out);
    }

    /**
     * Method to test: {@link BrowserAPIImpl#inParallelChunks(List, int, int, java.util.function.Function)}
     * Given scenario: more chunks than the permitted concurrency.
     * Expected result: never more than that many loaders in flight at once. With a loader that
     * holds a database connection, this bound is the whole point of the parameter.
     */
    @Test
    public void test_inParallelChunks_neverExceedsTheConcurrencyBound() {

        final int limit = 3;
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger peak = new AtomicInteger();

        BrowserAPIImpl.inParallelChunks(
                IntStream.rangeClosed(1, 60).boxed().toList(), 2, limit,
                chunk -> {
                    peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                    try {
                        Thread.sleep(20);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        inFlight.decrementAndGet();
                    }
                    return chunk;
                });

        assertTrue("peak " + peak.get() + " must not exceed " + limit, peak.get() <= limit);
        assertTrue("and the work must actually overlap", peak.get() > 1);
    }

    /**
     * Method to test: {@link BrowserAPIImpl#inParallelChunks(List, int, int, java.util.function.Function)}
     * Given scenario: any loader at all.
     * Expected result: it runs on a virtual thread, not on a platform pool thread. The loaders here
     * block on a socket, which is the case virtual threads are for.
     */
    @Test
    public void test_inParallelChunks_runsOnVirtualThreads() {

        final Set<Boolean> virtual = ConcurrentHashMap.newKeySet();

        BrowserAPIImpl.inParallelChunks(
                IntStream.rangeClosed(1, 12).boxed().toList(), 3, 4,
                chunk -> {
                    virtual.add(Thread.currentThread().isVirtual());
                    return chunk;
                });

        assertEquals("every loader ran on a virtual thread", Set.of(Boolean.TRUE), virtual);
    }

    /**
     * Method to test: {@link BrowserAPIImpl#inParallelChunks(List, int, int, java.util.function.Function)}
     * Given scenario: an empty input.
     * Expected result: no loader is invoked and the result is empty — no empty chunk reaches the
     * database.
     */
    @Test
    public void test_inParallelChunks_empty_neverCallsTheLoader() {

        final AtomicInteger calls = new AtomicInteger();

        final List<Integer> out = BrowserAPIImpl.inParallelChunks(
                List.<Integer>of(), 10, 4,
                chunk -> {
                    calls.incrementAndGet();
                    return chunk;
                });

        assertTrue(out.isEmpty());
        assertEquals(0, calls.get());
    }
}
