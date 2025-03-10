package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

/**
 * Test class for {@link CountVariantsInAllArchivedExperimentsMetricType}
 */
public class CountVariantsInAllArchivedExperimentsMetricTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link CountVariantsInAllArchivedExperimentsMetricType#getValue()}
     * Given Scenario: Creates two archived experiments with one variant each one
     * ExpectedResult: Returns at least two of them
     */
    @Test
    public void test_getvalue_trivial_case(){

        new ExperimentDataGen()
                .addVariant("Variant 1")
                .status(AbstractExperiment.Status.ARCHIVED)
                .nextPersisted();

        new ExperimentDataGen()
                .addVariant("Variant 2")
                .status(AbstractExperiment.Status.ARCHIVED)
                .nextPersisted();

        final Optional<Object> valueOpt = new CountVariantsInAllArchivedExperimentsMetricType().getValue();
        Assert.assertTrue("Should be not empty", valueOpt.isPresent());
        Assert.assertTrue("The number of variants on experiments ended should be at least two", Number.class.cast(valueOpt.get()).intValue() >= 2);
    }
}
