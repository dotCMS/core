package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Test class for {@link CountPagesWithScheduledExperimentsMetricType}
 * @since 5.3.5
 */
public class CountPagesWithScheduledExperimentsMetricTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link CountPagesWithScheduledExperimentsMetricType#getValue()}
     * Given Scenario: Add two experiments scheduled
     * ExpectedResult: Returns at least 2 of them
     */
    @Test
    public void test_getvalue_trivial_case(){

        new ExperimentDataGen()
                .status(AbstractExperiment.Status.SCHEDULED)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(10, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        new ExperimentDataGen()
                .status(AbstractExperiment.Status.SCHEDULED)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now())
                        .endDate(Instant.now().plus(11, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        final Optional<Object> valueOpt = new CountPagesWithScheduledExperimentsMetricType().getValue();
        Assert.assertTrue("Should be not empty", valueOpt.isPresent());
        Assert.assertTrue("The number of experiments scheduled should be at least two", Number.class.cast(valueOpt.get()).intValue() >= 2);
    }
}
