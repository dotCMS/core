package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

/**
 * Test class for {@link CountVariantsInAllEndedExperimentsMetricType}
 */
public class CountVariantsInAllEndedExperimentsMetricTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link CountVariantsInAllEndedExperimentsMetricType#getValue()}
     * Given Scenario: Creates two ended experiments with one variant each one
     * ExpectedResult: Returns at least two of them
     */
    @Test
    public void test_getvalue_trivial_case(){

        new ExperimentDataGen()
                .addVariant("Variant 1")
                .status(AbstractExperiment.Status.ENDED)
                .nextPersisted();

        new ExperimentDataGen()
                .addVariant("Variant 2")
                .status(AbstractExperiment.Status.ENDED)
                .nextPersisted();

        final Optional<Object> valueOpt = new CountVariantsInAllEndedExperimentsMetricType().getValue();
        Assert.assertTrue("Should be not empty", valueOpt.isPresent());
        Assert.assertTrue("The number of variants on experiments ended should be at least two", Number.class.cast(valueOpt.get()).intValue() >= 2);
    }
}
