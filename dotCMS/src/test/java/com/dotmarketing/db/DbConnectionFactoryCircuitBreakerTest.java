package com.dotmarketing.db;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for DatabaseConnectionHealthManager circuit breaker functionality
 * with DbConnectionFactory. Tests the complete fault tolerance system.
 * 
 * @author dotCMS
 */
@RunWith(MockitoJUnitRunner.class)
public class DbConnectionFactoryCircuitBreakerTest {

    private DatabaseConnectionHealthManager healthManager;
    private DatabaseConnectionHealthManager.CircuitState originalCircuitState;
    
    @Before
    public void setUp() {
        // Reset health manager for testing
        try {
            java.lang.reflect.Field instanceField = DatabaseConnectionHealthManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors for test setup
        }
        
        // Configure for faster testing
        try (MockedStatic<Config> configMock = mockStatic(Config.class)) {
            configMock.when(() -> Config.getIntProperty("DATABASE_CIRCUIT_BREAKER_FAILURE_THRESHOLD", 5))
                     .thenReturn(2); // Very low threshold for fast testing
            configMock.when(() -> Config.getIntProperty("DATABASE_CIRCUIT_BREAKER_RECOVERY_TIMEOUT_MS", 30000))
                     .thenReturn(500); // Short recovery timeout
            
            healthManager = DatabaseConnectionHealthManager.getInstance();
            originalCircuitState = healthManager.getHealthStatus().getCircuitState();
        }
    }
    
    @After
    public void tearDown() {
        // Reset circuit breaker to CLOSED state
        if (healthManager != null) {
            healthManager.closeCircuit("Test cleanup");
        }
    }
    
    @Test
    public void testDbConnectionFactoryRespectsCircuitBreakerOpen() {
        // Manually open circuit breaker
        healthManager.openCircuit("Test: simulating database unavailable");
        
        // Verify circuit is open
        assertFalse("Circuit breaker should be OPEN", healthManager.isOperationAllowed());
        
        // Try to get connection - should fail fast due to circuit breaker
        try {
            DbConnectionFactory.getConnection();
            fail("Expected DotRuntimeException due to circuit breaker being OPEN");
        } catch (DotRuntimeException e) {
            assertTrue("Error message should mention circuit breaker", 
                      e.getMessage().contains("circuit breaker is OPEN"));
        }
    }
    
    @Test
    public void testDbConnectionFactoryAllowsOperationsWhenCircuitClosed() {
        // Ensure circuit is closed
        healthManager.closeCircuit("Test: ensuring circuit is closed");
        
        // Verify circuit is closed
        assertTrue("Circuit breaker should be CLOSED", healthManager.isOperationAllowed());
        
        // Note: This test would need actual database connection for full integration
        // In unit test environment, we verify the circuit breaker check passes
        assertTrue("Should allow database operations when circuit is closed", 
                  DbConnectionFactory.isDatabaseOperationAllowed());
    }
    
    @Test
    public void testCircuitBreakerRecordsConnectionFailures() {
        // Ensure circuit starts closed
        healthManager.closeCircuit("Test setup");
        
        // Get initial failure count
        int initialFailures = healthManager.getHealthStatus().getConsecutiveFailures();
        
        // Simulate connection failure
        healthManager.recordFailure(new RuntimeException("Test connection failure"));
        
        // Verify failure was recorded
        int newFailures = healthManager.getHealthStatus().getConsecutiveFailures();
        assertEquals("Failure count should increment", initialFailures + 1, newFailures);
    }
    
    @Test
    public void testCircuitBreakerRecordsConnectionSuccess() {
        // Record a failure first
        healthManager.recordFailure(new RuntimeException("Test failure"));
        int failuresAfterError = healthManager.getHealthStatus().getConsecutiveFailures();
        assertTrue("Should have failures recorded", failuresAfterError > 0);
        
        // Record success
        healthManager.recordSuccess();
        
        // Verify success resets failure count
        int failuresAfterSuccess = healthManager.getHealthStatus().getConsecutiveFailures();
        assertEquals("Success should reset failure count to 0", 0, failuresAfterSuccess);
    }
    
    @Test
    public void testCircuitBreakerAutoRecoveryAfterTimeout() throws InterruptedException {
        // Trip the circuit breaker
        healthManager.recordFailure(new RuntimeException("Test failure 1"));
        healthManager.recordFailure(new RuntimeException("Test failure 2"));
        
        // Verify circuit is open
        assertFalse("Circuit should be OPEN after failures", healthManager.isOperationAllowed());
        assertEquals("Circuit state should be OPEN", 
                    DatabaseConnectionHealthManager.CircuitState.OPEN,
                    healthManager.getHealthStatus().getCircuitState());
        
        // Wait for recovery timeout (500ms in test config)
        Thread.sleep(600);
        
        // Circuit should now allow operations (transition to HALF_OPEN)
        assertTrue("Circuit should allow operations after recovery timeout", 
                  healthManager.isOperationAllowed());
        assertEquals("Circuit state should be HALF_OPEN after timeout", 
                    DatabaseConnectionHealthManager.CircuitState.HALF_OPEN,
                    healthManager.getHealthStatus().getCircuitState());
    }
    
    @Test
    public void testConnectionHealthStatusIntegration() {
        DatabaseConnectionHealthManager.HealthStatus status = DbConnectionFactory.getConnectionHealthStatus();
        
        assertNotNull("Health status should not be null", status);
        assertNotNull("Circuit state should not be null", status.getCircuitState());
        assertTrue("Consecutive failures should be >= 0", status.getConsecutiveFailures() >= 0);
        assertTrue("Connection leak count should be >= 0", status.getConnectionLeakCount() >= 0);
    }
    
    @Test
    public void testManualCircuitBreakerControlViaDbConnectionFactory() {
        // Test manual open
        DbConnectionFactory.openDatabaseCircuitBreaker("Integration test - manual open");
        
        assertFalse("Circuit should be OPEN after manual open", 
                   DbConnectionFactory.isDatabaseOperationAllowed());
        assertEquals("Circuit state should be OPEN", 
                    DatabaseConnectionHealthManager.CircuitState.OPEN,
                    DbConnectionFactory.getConnectionHealthStatus().getCircuitState());
        
        // Test manual close
        DbConnectionFactory.closeDatabaseCircuitBreaker("Integration test - manual close");
        
        assertTrue("Circuit should be CLOSED after manual close", 
                  DbConnectionFactory.isDatabaseOperationAllowed());
        assertEquals("Circuit state should be CLOSED", 
                    DatabaseConnectionHealthManager.CircuitState.CLOSED,
                    DbConnectionFactory.getConnectionHealthStatus().getCircuitState());
    }
    
    @Test
    public void testConcurrentConnectionAttemptsDuringCircuitBreakerOpen() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successfulConnections = new AtomicInteger(0);
        final AtomicInteger failedConnections = new AtomicInteger(0);
        
        // Open circuit breaker
        healthManager.openCircuit("Test: concurrent connection attempts");
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Submit concurrent connection attempts
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // This should fail fast due to circuit breaker
                    Connection conn = DbConnectionFactory.getConnection();
                    if (conn != null) {
                        successfulConnections.incrementAndGet();
                        conn.close();
                    }
                } catch (DotRuntimeException e) {
                    if (e.getMessage().contains("circuit breaker is OPEN")) {
                        failedConnections.incrementAndGet();
                    }
                } catch (SQLException e) {
                    // Handle conn.close() SQLException if needed
                    if (e.getMessage().contains("circuit breaker is OPEN")) {
                        failedConnections.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue("All threads should complete within timeout", latch.await(5, TimeUnit.SECONDS));
        
        // All connections should fail when circuit is open
        assertEquals("All connection attempts should fail when circuit is open", 
                    threadCount, failedConnections.get());
        assertEquals("No connections should succeed when circuit is open", 
                    0, successfulConnections.get());
        
        executor.shutdown();
    }
    
    @Test
    public void testCircuitBreakerStateTransitions() throws InterruptedException {
        // Start in CLOSED state
        healthManager.closeCircuit("Test setup");
        assertEquals("Should start in CLOSED state", 
                    DatabaseConnectionHealthManager.CircuitState.CLOSED,
                    healthManager.getHealthStatus().getCircuitState());
        
        // Transition to OPEN
        healthManager.recordFailure(new RuntimeException("Test failure 1"));
        healthManager.recordFailure(new RuntimeException("Test failure 2"));
        assertEquals("Should transition to OPEN after failures", 
                    DatabaseConnectionHealthManager.CircuitState.OPEN,
                    healthManager.getHealthStatus().getCircuitState());
        
        // Wait for recovery timeout
        Thread.sleep(600);
        
        // Should transition to HALF_OPEN
        assertTrue("Should allow operation after timeout", healthManager.isOperationAllowed());
        assertEquals("Should be in HALF_OPEN state", 
                    DatabaseConnectionHealthManager.CircuitState.HALF_OPEN,
                    healthManager.getHealthStatus().getCircuitState());
        
        // Success should transition back to CLOSED
        healthManager.recordSuccess();
        assertEquals("Should transition to CLOSED after success", 
                    DatabaseConnectionHealthManager.CircuitState.CLOSED,
                    healthManager.getHealthStatus().getCircuitState());
    }
    
    @Test
    public void testHealthStatusTimestamps() {
        long beforeFailure = System.currentTimeMillis();
        healthManager.recordFailure(new RuntimeException("Test failure with timestamp"));
        long afterFailure = System.currentTimeMillis();
        
        DatabaseConnectionHealthManager.HealthStatus status = healthManager.getHealthStatus();
        Instant lastFailureTime = status.getLastFailureTime();
        
        assertTrue("Last failure time should be recent", 
                  lastFailureTime != null && 
                  lastFailureTime.toEpochMilli() >= beforeFailure && 
                  lastFailureTime.toEpochMilli() <= afterFailure);
        
        long beforeSuccess = System.currentTimeMillis();
        healthManager.recordSuccess();
        long afterSuccess = System.currentTimeMillis();
        
        status = healthManager.getHealthStatus();
        Instant lastSuccessTime = status.getLastSuccessTime();
        
        assertTrue("Last success time should be recent", 
                  lastSuccessTime != null && 
                  lastSuccessTime.toEpochMilli() >= beforeSuccess && 
                  lastSuccessTime.toEpochMilli() <= afterSuccess);
    }
    
    @Test
    public void testCircuitBreakerWithDatabaseUnavailableSimulation() {
        // Simulate database completely unavailable
        healthManager.recordFailure(new RuntimeException("Database connection timeout"));
        healthManager.recordFailure(new RuntimeException("Database connection refused"));
        
        // Circuit should be open
        assertFalse("Circuit should be OPEN when database unavailable", 
                   healthManager.isOperationAllowed());
        
        // All subsequent operations should fail fast
        assertFalse("Should not allow operations when database unavailable", 
                   DbConnectionFactory.isDatabaseOperationAllowed());
    }
    
    @Test(timeout = 10000)
    public void testCircuitBreakerRecoveryScenario() throws InterruptedException {
        // 1. Simulate database going down
        healthManager.recordFailure(new RuntimeException("Database down - connection timeout"));
        healthManager.recordFailure(new RuntimeException("Database down - connection refused"));
        
        assertFalse("Circuit should be OPEN", healthManager.isOperationAllowed());
        
        // 2. Wait for recovery timeout
        Thread.sleep(600);
        
        // 3. Simulate database coming back up
        assertTrue("Should allow probe operation", healthManager.isOperationAllowed());
        healthManager.recordSuccess(); // Simulate successful probe
        
        // 4. Circuit should be closed and healthy
        assertTrue("Circuit should be CLOSED after recovery", healthManager.isOperationAllowed());
        assertEquals("Circuit state should be CLOSED", 
                    DatabaseConnectionHealthManager.CircuitState.CLOSED,
                    healthManager.getHealthStatus().getCircuitState());
        assertTrue("Health status should indicate healthy", 
                  healthManager.getHealthStatus().isHealthy());
    }
}