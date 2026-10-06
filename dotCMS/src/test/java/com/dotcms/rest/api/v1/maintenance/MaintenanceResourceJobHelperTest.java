package com.dotcms.rest.api.v1.maintenance;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link MaintenanceResource#jobHelper()} lazy-init method,
 * verifying that the double-checked locking pattern prevents multiple CDI bean
 * instantiations under concurrent access.
 */
class MaintenanceResourceJobHelperTest {

    /**
     * Verifies that {@code jobHelper()} resolves the CDI bean exactly once when
     * multiple threads call it simultaneously on the same resource instance.
     */
    @Test
    void jobHelper_shouldInitializeBeanExactlyOnce_underConcurrentAccess() throws Exception {
        final int threadCount = 20;
        final AtomicInteger callCount = new AtomicInteger(0);
        final MaintenanceJobHelper mockHelper = mock(MaintenanceJobHelper.class);

        // Subclass overrides CDI lookup; a sleep inside maximises the race window
        final MaintenanceResource resource = new MaintenanceResource(null) {
            @Override
            protected MaintenanceJobHelper resolveJobHelperBean() {
                callCount.incrementAndGet();
                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return mockHelper;
            }
        };

        final Method jobHelperMethod = MaintenanceResource.class.getDeclaredMethod("jobHelper");
        jobHelperMethod.setAccessible(true);

        final CyclicBarrier startGate = new CyclicBarrier(threadCount);
        final CountDownLatch done = new CountDownLatch(threadCount);
        final List<MaintenanceJobHelper> results = Collections.synchronizedList(new ArrayList<>());
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final Thread thread = new Thread(() -> {
                try {
                    startGate.await(5, TimeUnit.SECONDS); // release all threads at once
                    results.add((MaintenanceJobHelper) jobHelperMethod.invoke(resource));
                } catch (final Exception e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        done.await(15, TimeUnit.SECONDS);

        assertEquals(0, errors.size(), "No thread should have thrown: " + errors);
        assertEquals(threadCount, results.size(), "All threads should get a result");
        assertEquals(1, callCount.get(), "CDI bean must be resolved exactly once");
        results.forEach(h -> assertSame(mockHelper, h, "All threads must receive the same instance"));
    }

    /**
     * Verifies that a second call on the same resource returns the cached instance
     * without invoking the resolver again.
     */
    @Test
    void jobHelper_shouldReturnCachedInstance_onSubsequentCalls() throws Exception {
        final AtomicInteger callCount = new AtomicInteger(0);
        final MaintenanceJobHelper mockHelper = mock(MaintenanceJobHelper.class);

        final MaintenanceResource resource = new MaintenanceResource(null) {
            @Override
            protected MaintenanceJobHelper resolveJobHelperBean() {
                callCount.incrementAndGet();
                return mockHelper;
            }
        };

        final Method jobHelperMethod = MaintenanceResource.class.getDeclaredMethod("jobHelper");
        jobHelperMethod.setAccessible(true);

        final MaintenanceJobHelper first  = (MaintenanceJobHelper) jobHelperMethod.invoke(resource);
        final MaintenanceJobHelper second = (MaintenanceJobHelper) jobHelperMethod.invoke(resource);

        assertEquals(1, callCount.get(), "Resolver must only be called once");
        assertSame(first, second, "Both calls must return the same instance");
    }
}
