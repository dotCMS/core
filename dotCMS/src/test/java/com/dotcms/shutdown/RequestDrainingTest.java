package com.dotcms.shutdown;

import com.dotmarketing.util.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test class to manually verify request draining functionality.
 *
 * This test simulates active requests during shutdown to verify that:
 * 1. Request draining waits for active requests to complete
 * 2. Shutdown proceeds after requests finish
 * 3. Timeout behavior works correctly
 *
 * To test manually:
 * 1. Run the test method
 * 2. While it's running, send SIGTERM to the process
 * 3. Observe the shutdown logs showing request draining
 */
@Ignore("This test is for manual verification of request draining and should not be run in CI/CD")
public class RequestDrainingTest {

    @Test
    public void testRequestDrainingManual() throws Exception {
        System.out.println("=== Request Draining Test ===");
        System.out.println("This test simulates long-running requests during shutdown.");
        System.out.println("To test manually:");
        System.out.println("1. Run this test");
        System.out.println("2. Send SIGTERM signal to the process while it's running");
        System.out.println("3. Observe the shutdown logs showing request draining");
        System.out.println("4. Watch how shutdown waits for requests to complete");
        System.out.println();

        // Simulate 3 long-running requests
        simulateLongRunningRequests(3, 10000); // 3 requests, 10 seconds each

        System.out.println("Test completed. Check the shutdown logs for request draining behavior.");
    }

    /**
     * Test request draining with timeout scenario
     */
    @Test
    public void testRequestDrainingTimeout() throws Exception {
        System.out.println("=== Request Draining Timeout Test ===");
        System.out.println("This test simulates requests that exceed the drain timeout.");
        System.out.println("Expected behavior: Shutdown should proceed after timeout even with active requests.");
        System.out.println();

        // Simulate requests that will exceed the default 15-second drain timeout
        simulateLongRunningRequests(2, 20000); // 2 requests, 20 seconds each

        System.out.println("Test completed. Check logs for timeout behavior.");
    }

    /**
     * Simulates multiple long-running requests by incrementing the active request counter
     */
    private void simulateLongRunningRequests(int requestCount, long durationMs) throws InterruptedException {
        CountDownLatch allRequestsStarted = new CountDownLatch(requestCount);
        CountDownLatch shutdownSignal = new CountDownLatch(1);
        AtomicBoolean shutdownDetected = new AtomicBoolean(false);

        System.out.printf("Starting %d simulated requests (duration: %dms each)%n", requestCount, durationMs);

        // Start the simulated requests
        for (int i = 0; i < requestCount; i++) {
            final int requestId = i + 1;
            CompletableFuture.runAsync(() -> {
                try {
                    // Increment active request count (like RequestTrackingFilter does)
                    ShutdownCoordinator.incrementActiveRequests();

                    System.out.printf("Request %d started (active requests: %d)%n",
                        requestId, ShutdownCoordinator.getCurrentActiveRequestCount());

                    allRequestsStarted.countDown();

                    // Simulate request processing time
                    long startTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startTime < durationMs) {
                        // Check if shutdown is in progress
                        if (ShutdownCoordinator.isRequestDraining() && !shutdownDetected.get()) {
                            shutdownDetected.set(true);
                            shutdownSignal.countDown();
                            System.out.printf("Request %d detected shutdown signal - will complete in %dms%n",
                                requestId, durationMs - (System.currentTimeMillis() - startTime));
                        }

                        Thread.sleep(100); // Check every 100ms
                    }

                    System.out.printf("Request %d completed%n", requestId);

                } catch (InterruptedException e) {
                    System.out.printf("Request %d interrupted%n", requestId);
                    Thread.currentThread().interrupt();
                } finally {
                    // Decrement active request count (like RequestTrackingFilter does)
                    ShutdownCoordinator.decrementActiveRequests();
                    System.out.printf("Request %d finished (active requests: %d)%n",
                        requestId, ShutdownCoordinator.getCurrentActiveRequestCount());
                }
            });
        }

        // Wait for all requests to start
        allRequestsStarted.await(5, TimeUnit.SECONDS);

        System.out.printf("All %d requests are now active. Current active count: %d%n",
            requestCount, ShutdownCoordinator.getCurrentActiveRequestCount());
        System.out.println();
        System.out.println("*** Send SIGTERM signal now to test request draining ***");
        System.out.println("Example: kill -TERM <pid>");
        System.out.println("Or use Docker: docker stop <container>");
        System.out.println();

        // Monitor shutdown status
        while (!ShutdownCoordinator.isRequestDraining()) {
            System.out.printf("Waiting for shutdown signal... (active requests: %d)%n",
                ShutdownCoordinator.getCurrentActiveRequestCount());
            Thread.sleep(2000);
        }

        System.out.println("*** SHUTDOWN DETECTED - Request draining started ***");

        // Wait for shutdown signal to be detected by at least one request
        shutdownSignal.await(30, TimeUnit.SECONDS);

        // Monitor the draining process
        while (ShutdownCoordinator.isRequestDraining()) {
            System.out.printf("Request draining in progress... (active requests: %d)%n",
                ShutdownCoordinator.getCurrentActiveRequestCount());
            Thread.sleep(1000);
        }

        System.out.println("Request draining completed!");

        // Give a bit more time for the test to complete
        Thread.sleep(2000);
    }
} 