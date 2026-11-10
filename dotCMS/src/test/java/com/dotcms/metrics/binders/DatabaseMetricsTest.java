package com.dotcms.metrics.binders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.health.checks.cdi.DatabaseHealthCheck;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.service.HealthStateManager;
import com.dotmarketing.db.DbConnectionFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Optional;
import javax.management.MBeanServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Scraping database health must read managed cached state without polling a database connection. */
class DatabaseMetricsTest {

    private final HealthStateManager health = mock(HealthStateManager.class);
    private final HikariDataSource dataSource = mock(HikariDataSource.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private MockedStatic<HealthStateManager> healthLocator;
    private MockedStatic<DbConnectionFactory> database;
    private MockedStatic<ManagementFactory> management;
    private MockedConstruction<DatabaseHealthCheck> standaloneChecks;
    private DatabaseMetrics metrics;

    /** Uses cached health and mock pool metadata without starting health workers or database services. */
    @BeforeEach
    void setUp() {
        healthLocator = mockStatic(HealthStateManager.class);
        healthLocator.when(HealthStateManager::getInstance).thenReturn(health);
        when(health.getHealthCheckResult("database")).thenReturn(Optional.of(result(HealthStatus.UP)));
        database = mockStatic(DbConnectionFactory.class);
        database.when(DbConnectionFactory::getDataSource).thenReturn(dataSource);
        when(dataSource.getPoolName()).thenReturn("metrics-test");
        management = mockStatic(ManagementFactory.class);
        management.when(ManagementFactory::getPlatformMBeanServer).thenReturn(mock(MBeanServer.class));
        standaloneChecks = mockConstruction(DatabaseHealthCheck.class,
                (check, context) -> when(check.check()).thenReturn(result(HealthStatus.UP)));
        metrics = new DatabaseMetrics();
        metrics.bindTo(registry);
    }

    /** Restores all factories and removes meters after each isolated scrape test. */
    @AfterEach
    void tearDown() {
        registry.close();
        standaloneChecks.close();
        management.close();
        database.close();
        healthLocator.close();
    }

    /** Repeated scrapes must not construct a standalone check or borrow a connection. */
    @Test
    void healthGauge_doesNotPollDatabase() throws Exception {
        assertEquals(1.0, value(), 0.001);
        assertEquals(1.0, value(), 0.001);
        assertTrue(standaloneChecks.constructed().isEmpty());
        verify(dataSource, never()).getConnection();
    }

    /** New managed results are reflected without adding a second health-check cache. */
    @Test
    void healthGauge_tracksManagedStatus() {
        for (final HealthStatus status : HealthStatus.values()) {
            when(health.getHealthCheckResult("database")).thenReturn(Optional.of(result(status)));
            if (status == HealthStatus.UNKNOWN) {
                assertTrue(Double.isNaN(value()));
            } else {
                assertEquals(status == HealthStatus.UP ? 1.0 : 0.0, value(), 0.001, status.name());
            }
        }
    }

    /** A not-yet-published result is unknown rather than a reason to run an inline database probe. */
    @Test
    void healthGauge_missingManagedResultIsUnknown() {
        when(health.getHealthCheckResult("database")).thenReturn(Optional.empty());

        assertTrue(Double.isNaN(value()));
        assertTrue(standaloneChecks.constructed().isEmpty());
    }

    /** Health-state lookup errors must not invent healthy or unhealthy database measurements. */
    @Test
    void healthGauge_unavailableManagedStateIsUnknown() {
        when(health.getHealthCheckResult("database")).thenThrow(new IllegalStateException("unavailable"));

        assertTrue(Double.isNaN(value()));
    }

    /** Reads the public gauge instead of a private health-status helper. */
    private double value() {
        return registry.get("dotcms.database.health.available").gauge().value();
    }

    /** Builds a timestamped result without executing the database check. */
    private HealthCheckResult result(final HealthStatus status) {
        return HealthCheckResult.builder().name("database").status(status).lastChecked(Instant.EPOCH).build();
    }
}
