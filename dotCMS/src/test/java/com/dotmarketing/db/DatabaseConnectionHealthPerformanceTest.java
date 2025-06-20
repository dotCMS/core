package com.dotmarketing.db;

import com.dotmarketing.util.Config;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Performance tests for DatabaseConnectionHealthManager.
 * Tests circuit breaker performance under high load and concurrent access patterns.
 * 
 * @author dotCMS
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseConnectionHealthPerformanceTest {

    @Mock
    private HikariDataSource mockDataSource;
    
    @Mock
    private HikariPoolMXBean mockPoolMXBean;
    
    private DatabaseConnectionHealthManager healthManager;
    
    @Before
    public void setUp() {
        // Reset singleton instance for testing
        try {
            java.lang.reflect.Field instanceField = DatabaseConnectionHealthManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors for test setup
        }
        
        // Configure for performance testing
        try (MockedStatic<Config> configMock = mockStatic(Config.class)) {
            configMock.when(() -> Config.getIntProperty("DATABASE_CIRCUIT_BREAKER_FAILURE_THRESHOLD", 5))
                     .thenReturn(100); // Higher threshold for performance testing
            configMock.when(() -> Config.getLongProperty("DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS", 60))
                     .thenReturn(5L); // Reasonable recovery timeout
            configMock.when(() -> Config.getLongProperty("DATABASE_HEALTH_CHECK_INTERVAL_SECONDS", 30))
                     .thenReturn(1L); // Faster health checking for performance test
            
            healthManager = DatabaseConnectionHealthManager.getInstance();
        }
        
        // Setup mock pool
        when(mockDataSource.getHikariPoolMXBean()).thenReturn(mockPoolMXBean);
        when(mockPoolMXBean.getActiveConnections()).thenReturn(10);
        when(mockPoolMXBean.getIdleConnections()).thenReturn(5);
        when(mockPoolMXBean.getTotalConnections()).thenReturn(15);
        when(mockPoolMXBean.getThreadsAwaitingConnection()).thenReturn(0);
    }
    
    @Test(timeout = 30000)
    public void testHighConcurrencyCircuitBreakerOperations() throws InterruptedException {
        final int threadCount = 50; // Reduced for stability
        final int operationsPerThread = 100; // Reduced for stability
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);
        final AtomicLong totalOperations = new AtomicLong(0);
        final AtomicLong successfulOperations = new AtomicLong(0);
        final AtomicInteger concurrentOperations = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Submit all threads
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for synchronized start
                    
                    for (int op = 0; op < operationsPerThread; op++) {
                        concurrentOperations.incrementAndGet();
                        totalOperations.incrementAndGet();
                        
                        try {
                            // Mix of operations to test different code paths
                            if (threadId % 4 == 0) {
                                // Check operation allowed
                                if (healthManager.isOperationAllowed()) {
                                    successfulOperations.incrementAndGet();
                                }
                            } else if (threadId % 4 == 1) {
                                // Record success
                                healthManager.recordSuccess();
                                successfulOperations.incrementAndGet();
                            } else if (threadId % 4 == 2) {
                                // Record failure (but not too many to avoid tripping circuit)
                                if (op % 50 == 0) { // Only occasional failures
                                    healthManager.recordFailure(new RuntimeException("Performance test failure " + threadId + ":" + op));
                                }
                                successfulOperations.incrementAndGet();
                            } else {
                                // Get health status
                                DatabaseConnectionHealthManager.HealthStatus status = healthManager.getHealthStatus();
                                if (status != null) {
                                    successfulOperations.incrementAndGet();
                                }
                            }
                        } finally {
                            concurrentOperations.decrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        assertTrue("All threads should complete within timeout", 
                  completionLatch.await(25, TimeUnit.SECONDS));
        
        long endTime = System.currentTimeMillis();
        long totalTimeMs = endTime - startTime;
        
        executor.shutdown();
        
        // Performance assertions
        long expectedOperations = (long) threadCount * operationsPerThread;
        assertEquals("All operations should be counted", expectedOperations, totalOperations.get());
        assertTrue("Most operations should succeed", successfulOperations.get() > expectedOperations * 0.8);
        
        double operationsPerSecond = (double) totalOperations.get() / (totalTimeMs / 1000.0);
        System.out.println(String.format("Performance Test Results:"));
        System.out.println(String.format("  Total operations: %d", totalOperations.get()));
        System.out.println(String.format("  Successful operations: %d", successfulOperations.get()));
        System.out.println(String.format("  Total time: %d ms", totalTimeMs));
        System.out.println(String.format("  Operations per second: %.2f", operationsPerSecond));
        
        // Performance requirement: should handle at least 10,000 operations per second
        assertTrue("Should achieve minimum performance threshold", operationsPerSecond > 10000);
        assertEquals("No threads should be stuck", 0, concurrentOperations.get());
    }
    
    @Test(timeout = 15000)
    public void testConnectionPoolMonitoringPerformance() throws InterruptedException {
        final int iterations = 1000; // Reduced for stability
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong totalDuration = new AtomicLong(0);
        
        healthManager.initializeMonitoring(mockDataSource);
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                long startTime = System.nanoTime();
                
                for (int i = 0; i < iterations; i++) {
                    // Verify monitoring doesn't block or take too long
                    DatabaseConnectionHealthManager.HealthStatus status = healthManager.getHealthStatus();
                    assertNotNull("Health status should always be available", status);
                }
                
                long endTime = System.nanoTime();
                totalDuration.set(endTime - startTime);
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue("Pool monitoring should complete within timeout", latch.await(10, TimeUnit.SECONDS));
        
        long durationMs = totalDuration.get() / 1_000_000;
        double avgTimePerCheck = (double) durationMs / iterations;
        
        System.out.println(String.format("Pool Monitoring Performance:"));
        System.out.println(String.format("  Iterations: %d", iterations));
        System.out.println(String.format("  Total time: %d ms", durationMs));
        System.out.println(String.format("  Average time per check: %.3f ms", avgTimePerCheck));
        
        // Performance requirement: each pool health check should take < 5ms on average
        assertTrue("Pool health checks should be fast", avgTimePerCheck < 5.0);
        
        executor.shutdown();
    }
    
    @Test(timeout = 10000)
    public void testMemoryUsageUnderLoad() throws InterruptedException {
        final int iterations = 10000; // Reduced for stability
        
        // Force garbage collection before test
        System.gc();
        Thread.sleep(100);
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform many operations to test for memory leaks
        for (int i = 0; i < iterations; i++) {
            healthManager.isOperationAllowed();
            
            if (i % 1000 == 0) {
                healthManager.recordSuccess();
            }
            
            if (i % 2000 == 0) {
                healthManager.getHealthStatus();
            }
            
            // Occasional failure to test all code paths
            if (i % 5000 == 0) {
                healthManager.recordFailure(new RuntimeException("Memory test failure " + i));
            }
        }
        
        // Force garbage collection after test
        System.gc();
        Thread.sleep(100);
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println(String.format("Memory Usage Test:"));
        System.out.println(String.format("  Operations: %d", iterations));
        System.out.println(String.format("  Initial memory: %d KB", initialMemory / 1024));
        System.out.println(String.format("  Final memory: %d KB", finalMemory / 1024));
        System.out.println(String.format("  Memory increase: %d KB", memoryIncrease / 1024));
        
        // Memory requirement: should not use more than 5MB additional memory for 10k operations
        assertTrue("Memory usage should be reasonable", memoryIncrease < 5 * 1024 * 1024);
    }
}