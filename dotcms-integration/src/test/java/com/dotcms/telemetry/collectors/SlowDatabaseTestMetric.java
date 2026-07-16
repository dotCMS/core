package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

/**
 * Test metric that executes a slow database query to validate timeout handling
 * and database connection management.
 *
 * <p>This metric uses PostgreSQL's pg_sleep() function to create a query that
 * takes longer than the configured timeout, providing a reliable way to test:
 * <ul>
 *   <li>Timeout enforcement works correctly</li>
 *   <li>Database connections are properly wrapped in {@code wrapConnection()}</li>
 *   <li>Connections are returned to pool even when timeout fires</li>
 *   <li>Race condition fixes prevent connection leaks</li>
 * </ul>
 * </p>
 *
 * <p><b>Why use pg_sleep() instead of Thread.sleep():</b><br>
 * Thread.sleep() tests the timeout mechanism but doesn't test database connection
 * management. Using pg_sleep() ensures we're actually holding a database connection
 * during the timeout, which tests the critical {@link com.dotmarketing.db.DbConnectionFactory#wrapConnection}
 * fix that prevents connection leaks.</p>
 *
 * @see MetricTimeoutTest
 * @see DBMetricType
 */
@MetricsProfile({ProfileType.MINIMAL, ProfileType.FULL})
@ApplicationScoped
public class SlowDatabaseTestMetric implements DBMetricType {

    @Override
    public String getName() {
        return "test.slow.database.metric";
    }

    @Override
    public String getDescription() {
        return "Test metric that executes a slow database query exceeding timeout";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.EXPERIMENTS;
    }

    @Override
    public String getSqlQuery() {
        // PostgreSQL pg_sleep() function sleeps for 3 seconds while holding the database connection
        // This tests:
        // 1. Database connection is acquired via wrapConnection()
        // 2. Timeout fires at 1 second while query is still running
        // 3. Connection cleanup happens correctly even when interrupted
        // 4. No connection leak occurs
        Logger.info(this, "SlowDatabaseTestMetric: Executing 3-second pg_sleep() query");
        return "SELECT pg_sleep(3), 999 as value";
    }
}