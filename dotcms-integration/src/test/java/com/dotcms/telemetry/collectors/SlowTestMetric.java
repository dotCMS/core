package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Test metric that intentionally takes longer than the timeout.
 * Used by {@link MetricTimeoutTest} to validate timeout handling.
 *
 * <p>This metric sleeps for 3 seconds, which exceeds the 1-second timeout
 * configured in the test setup, providing a reliable way to trigger timeout
 * scenarios without depending on narrow race timing windows.</p>
 *
 * <p><b>Note on Race Condition Testing:</b> The actual race condition (where
 * a metric completes just after timeout fires) is timing-dependent and difficult
 * to reproduce reliably. This explains why production saw only 30+ leaked connections
 * over weeks. Our fixes prevent the leak even when the race occurs.</p>
 */
@MetricsProfile({ProfileType.MINIMAL, ProfileType.FULL})
@ApplicationScoped
public class SlowTestMetric implements MetricType {

    @Override
    public String getName() {
        return "test.slow.metric";
    }

    @Override
    public String getDescription() {
        return "Test metric that intentionally exceeds timeout";
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
    public Optional<Object> getValue() {
        try {
            // Sleep for 3 seconds - reliably exceeds 1 second timeout
            Logger.info(this, "SlowTestMetric: Starting 3-second sleep");
            Thread.sleep(3000);
            Logger.info(this, "SlowTestMetric: Sleep completed (should be interrupted)");
            return Optional.of(999);
        } catch (InterruptedException e) {
            Logger.info(this, "SlowTestMetric: Interrupted as expected");
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}