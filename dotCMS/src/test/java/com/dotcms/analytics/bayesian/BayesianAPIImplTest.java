package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.BayesianResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BayesianAPIImplTest {

    private BayesianAPI bayesianAPI;

    @Before
    public void setup() {
        bayesianAPI = new BayesianAPIImpl();
    }

    @Test
    public void test_calculateABTesting() {
        final BayesianInput input = BayesianInput.builder()
                .priorAlpha(10)
                .priorBeta(10)
                .controlSuccesses(5)
                .controlFailures(3)
                .testSuccesses(6)
                .testFailures(2)
                .build();
        final BayesianResult result = bayesianAPI.calcABTesting(input);
        Assert.assertNotNull(result);
        Assert.assertEquals("0.72", String.format("%.2f", result.result()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorAlpha() {
        BayesianInput input = BayesianInput.builder()
                .priorAlpha(0)
                .priorBeta(10)
                .controlSuccesses(5)
                .controlFailures(3)
                .testSuccesses(5)
                .testFailures(3)
                .build();
        bayesianAPI.calcABTesting(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidPriorBeta() {
        BayesianInput input = BayesianInput.builder()
                .priorAlpha(10)
                .priorBeta(0)
                .controlSuccesses(5)
                .controlFailures(3)
                .testSuccesses(5)
                .testFailures(3)
                .build();
        bayesianAPI.calcABTesting(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidControlSuccesses() {
        BayesianInput input = BayesianInput.builder()
                .priorAlpha(10)
                .priorBeta(10)
                .controlSuccesses(-1)
                .controlFailures(3)
                .testSuccesses(5)
                .testFailures(3)
                .build();
        bayesianAPI.calcABTesting(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidControlFailures() {
        BayesianInput input = BayesianInput.builder()
                .priorAlpha(10)
                .priorBeta(10)
                .controlSuccesses(5)
                .controlFailures(-1)
                .testSuccesses(5)
                .testFailures(3)
                .build();
        bayesianAPI.calcABTesting(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidTestSuccesses() {
        BayesianInput input = BayesianInput.builder()
                .priorAlpha(10)
                .priorBeta(10)
                .controlSuccesses(5)
                .controlFailures(3)
                .testSuccesses(-1)
                .testFailures(3)
                .build();
        bayesianAPI.calcABTesting(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_calculateABTesting_invalidTestFailures() {
        BayesianInput input = BayesianInput.builder()
                .priorAlpha(10)
                .priorBeta(10)
                .controlSuccesses(5)
                .controlFailures(3)
                .testSuccesses(5)
                .testFailures(-1)
                .build();
        bayesianAPI.calcABTesting(input);
    }

}
