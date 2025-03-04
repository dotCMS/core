package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

/**
 * Test class for {@link ExperimentFeatureFlagMetricType}
 * @since 5.3.5
 */
public class ExperimentFeatureFlagMetricTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentFeatureFlagMetricType#getValue()}
     * Given Scenario: Test if retrieves a boolean value
     * ExpectedResult: Returns a boolean value
     */
    @Test
    public void test_experiment_feature_flag_metric_type_getvalue_trivial_case() {

        final Optional<Object> valueOpt = new ExperimentFeatureFlagMetricType().getValue();
        Assert.assertTrue("Should be not empty", valueOpt.isPresent());
        Assert.assertTrue("Should be a boolean", valueOpt.get() instanceof Boolean);
    }
}
