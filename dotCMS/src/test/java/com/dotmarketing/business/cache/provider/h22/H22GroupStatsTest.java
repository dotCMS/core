package com.dotmarketing.business.cache.provider.h22;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class H22GroupStatsTest {

    /** Concurrent async writers must not lose bytes, write counts or timings. */
    @Test
    public void testConcurrentAccountingPreservesEveryUpdate() throws Exception {
        final H22GroupStats stats = new H22GroupStats("concurrent");
        final int threads = 8;
        final int writesPerThread = 20000;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int thread = 0; thread < threads; thread++) {
                tasks.add(executor.submit(() -> {
                    start.await();
                    for (int write = 0; write < writesPerThread; write++) {
                        stats.writeSize(1024);
                        stats.writeTime(10);
                        stats.hitOrMiss("hit");
                        stats.hitOrMiss(null);
                        stats.readTime(5);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (final Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
            final long operations = (long) threads * writesPerThread;
            assertEquals(operations, counter(stats, "writes"));
            assertEquals(operations * 1024, counter(stats, "totalSize"));
            assertEquals(operations * 10, counter(stats, "totalTimeWriting"));
            assertEquals(operations, counter(stats, "hits"));
            assertEquals(operations, counter(stats, "misses"));
            assertEquals(operations * 5, counter(stats, "totalTimeReading"));
        } finally {
            executor.shutdownNow();
        }
    }

    /** Reads either a legacy boxed long or its thread-safe Number replacement. */
    private long counter(final H22GroupStats stats, final String name) throws Exception {
        final Field field = H22GroupStats.class.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(stats)).longValue();
    }
}
