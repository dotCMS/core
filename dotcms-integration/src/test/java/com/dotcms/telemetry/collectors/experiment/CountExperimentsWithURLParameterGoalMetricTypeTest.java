package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.model.GoalFactory;
import com.dotcms.experiments.model.Goals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

/**
 * Test class for {@link CountExperimentsWithURLParameterGoalMetricType}
 */
public class CountExperimentsWithURLParameterGoalMetricTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link CountExperimentsWithURLParameterGoalMetricType#getValue()}
     * Given Scenario: Creates one experiments with url param goal
     * ExpectedResult: Returns at least one of them
     */
    @Test
    public void test_getvalue_trivial_case(){

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset experimentPage = new HTMLPageDataGen(host, template).nextPersisted();
        final Metric metric = Metric.builder()
                .name("Testing url param Metric")
                .type(MetricType.URL_PARAMETER)
                .build();

        final Goals goal = Goals.builder().primary(GoalFactory.create(metric)).build();
        new ExperimentDataGen()
                .page(experimentPage)
                .addGoal(goal).nextPersisted();


        final Optional<Object> valueOpt = new CountExperimentsWithURLParameterGoalMetricType().getValue();
        Assert.assertTrue("Should be not empty", valueOpt.isPresent());
        Assert.assertTrue("The number of experiments with an url param goal should be at least one", Number.class.cast(valueOpt.get()).intValue() >= 1);
    }
}
