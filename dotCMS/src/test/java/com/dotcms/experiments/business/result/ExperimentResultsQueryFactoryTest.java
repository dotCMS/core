package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)

/**
 * Unit tests for {@link ExperimentResultsQueryFactory} dispatch logic.
 *
 * Verifies that both {@code executeByDay} and {@code executeAggregate} route to the correct
 * implementation based on the {@code FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS} flag state, for all
 * four supported goal types (BOUNCE_RATE, EXIT_RATE, REACH_PAGE, URL_PARAMETER).
 */
public class ExperimentResultsQueryFactoryTest {

    @Before
    public void setUp() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);
    }

    @After
    public void tearDown() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);
    }

    // -------------------------------------------------------------------------
    // Flag DISABLED — all four goal types should resolve to CubeJSGoalResultsAdapter
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Flag DISABLED — all four goal types should resolve to CubeJSGoalResultsAdapter
    // -------------------------------------------------------------------------

    @Test
    public void executeByDay_flagDisabled_bounceRate_returnsCubeJSAdapterWithCorrectMetricQuery() {
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE);
        assertNotNull(impl);
        assertTrue("Expected CubeJSGoalResultsAdapter when flag is disabled",
                impl instanceof CubeJSGoalResultsAdapter);
        assertTrue("Expected BounceRateResultQuery wrapped inside the adapter",
                ((CubeJSGoalResultsAdapter) impl).getMetricQuery() instanceof BounceRateResultQuery);
    }

    @Test
    public void executeByDay_flagDisabled_exitRate_returnsCubeJSAdapterWithCorrectMetricQuery() {
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.EXIT_RATE);
        assertTrue(impl instanceof CubeJSGoalResultsAdapter);
        assertTrue(((CubeJSGoalResultsAdapter) impl).getMetricQuery() instanceof ExitRateResultQuery);
    }

    @Test
    public void executeByDay_flagDisabled_reachPage_returnsCubeJSAdapterWithCorrectMetricQuery() {
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.REACH_PAGE);
        assertTrue(impl instanceof CubeJSGoalResultsAdapter);
        assertTrue(((CubeJSGoalResultsAdapter) impl).getMetricQuery()
                instanceof ReachTargetAfterExperimentPageResultQuery);
    }

    @Test
    public void executeByDay_flagDisabled_urlParameter_returnsCubeJSAdapterWithCorrectMetricQuery() {
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.URL_PARAMETER);
        assertTrue(impl instanceof CubeJSGoalResultsAdapter);
        assertTrue(((CubeJSGoalResultsAdapter) impl).getMetricQuery()
                instanceof ReachTargetAfterExperimentPageResultQuery);
    }

    // -------------------------------------------------------------------------
    // Flag ENABLED — all four goal types should resolve to CAEM implementations
    // -------------------------------------------------------------------------

    @Test
    public void executeByDay_flagEnabled_bounceRate_returnsCaemImpl() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE);
        assertNotNull(impl);
        assertTrue("Expected BounceRateCAEMResultQuery when flag is enabled",
                impl instanceof BounceRateCAEMResultQuery);
    }

    @Test
    public void executeByDay_flagEnabled_exitRate_returnsCaemImpl() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.EXIT_RATE);
        assertTrue(impl instanceof ExitRateCAEMResultQuery);
    }

    @Test
    public void executeByDay_flagEnabled_reachPage_returnsCaemImpl() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.REACH_PAGE);
        assertTrue(impl instanceof ReachPageCAEMResultQuery);
    }

    @Test
    public void executeByDay_flagEnabled_urlParameter_returnsCaemImpl() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.URL_PARAMETER);
        assertTrue(impl instanceof UrlParameterCAEMResultQuery);
    }

    // -------------------------------------------------------------------------
    // Flag toggled at runtime — next call reflects the change without restart
    // -------------------------------------------------------------------------

    @Test
    public void resolveImpl_flagToggledAtRuntime_reflectsChangeImmediately() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);
        assertTrue(ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE)
                instanceof CubeJSGoalResultsAdapter);

        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);
        assertTrue(ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE)
                instanceof BounceRateCAEMResultQuery);

        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);
        assertTrue(ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE)
                instanceof CubeJSGoalResultsAdapter);
    }

}
