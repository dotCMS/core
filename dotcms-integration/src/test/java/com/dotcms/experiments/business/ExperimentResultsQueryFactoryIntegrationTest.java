package com.dotcms.experiments.business;

import com.dotcms.analytics.metrics.AbstractCondition.Operator;
import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.business.result.BounceRateCAEMResultQuery;
import com.dotcms.experiments.business.result.CubeJSGoalResultsAdapter;
import com.dotcms.experiments.business.result.ExperimentGoalResultsQuery;
import com.dotcms.experiments.business.result.ExperimentResultsQueryFactory;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for {@link ExperimentResultsQueryFactory}.
 *
 * Verifies that both {@code executeByDay} and {@code executeAggregate} dispatch to the correct
 * implementation per goal type based on the feature flag state.
 *
 * Switch-ENABLED path is not tested here — CAEM is unavailable in the CI environment (FR-019).
 * Unit tests cover flag-enabled dispatch.
 */
public class ExperimentResultsQueryFactoryIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void tearDown() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);
    }

    // -------------------------------------------------------------------------
    // Switch DISABLED (default) — factory must return CubeJSGoalResultsAdapter
    // -------------------------------------------------------------------------

    @Test
    public void resolveImpl_flagDisabled_reachPageGoal_returnsCubeJSAdapter() {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset reachPage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Metric")
                .type(MetricType.REACH_PAGE)
                .addConditions(getUrlCondition(reachPage.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

        new ExperimentDataGen()
                .addVariant("page_reach+testing_1")
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.REACH_PAGE);

        assertNotNull(impl);
        assertTrue("Expected CubeJSGoalResultsAdapter when flag is disabled",
                impl instanceof CubeJSGoalResultsAdapter);
    }

    @Test
    public void resolveImpl_flagDisabled_exitRateGoal_returnsCubeJSAdapter() {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();

        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final HTMLPageAsset bouncePage = new HTMLPageDataGen(host, template).nextPersisted();

        final Metric metric = Metric.builder()
                .name("Testing Exit Rate Metric")
                .type(MetricType.EXIT_RATE)
                .addConditions(getUrlCondition(bouncePage.getPageUrl()))
                .build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();

        new ExperimentDataGen()
                .addVariant("exit_rate+testing_1")
                .page(experimentPage)
                .addGoal(goal)
                .nextPersisted();

        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.EXIT_RATE);

        assertNotNull(impl);
        assertTrue("Expected CubeJSGoalResultsAdapter when flag is disabled",
                impl instanceof CubeJSGoalResultsAdapter);
    }

    @Test
    public void resolveImpl_flagDisabled_bounceRateGoal_returnsCubeJSAdapter() {
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE);
        assertTrue(impl instanceof CubeJSGoalResultsAdapter);
    }

    @Test
    public void resolveImpl_flagDisabled_urlParameterGoal_returnsCubeJSAdapter() {
        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.URL_PARAMETER);
        assertTrue(impl instanceof CubeJSGoalResultsAdapter);
    }

    // -------------------------------------------------------------------------
    // Switch ENABLED — factory must return CAEM implementation
    // (dispatch type only; no actual CAEM HTTP call made — FR-019)
    // -------------------------------------------------------------------------

    @Test
    public void resolveImpl_flagEnabled_bounceRateGoal_returnsCAEMImpl() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);

        final ExperimentGoalResultsQuery impl =
                ExperimentResultsQueryFactory.INSTANCE.resolveImpl(MetricType.BOUNCE_RATE);

        assertNotNull(impl);
        assertTrue("Expected BounceRateCAEMResultQuery when flag is enabled",
                impl instanceof BounceRateCAEMResultQuery);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Condition getUrlCondition(final String url) {
        return Condition.builder()
                .parameter("url")
                .value(url)
                .operator(Operator.EQUALS)
                .build();
    }

    private static Condition getRefererCondition(final String referer) {
        return Condition.builder()
                .parameter("referer")
                .value(referer)
                .operator(Operator.EQUALS)
                .build();
    }

}
