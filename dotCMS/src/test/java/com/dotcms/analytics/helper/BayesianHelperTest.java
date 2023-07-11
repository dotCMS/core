package com.dotcms.analytics.helper;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.bayesian.model.ABTestingType;
import com.dotcms.analytics.bayesian.model.BayesianInput;
import com.dotcms.analytics.bayesian.model.VariantInput;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.result.GoalResults;
import com.dotcms.experiments.business.result.VariantResults;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.dotcms.experiments.business.ExperimentsAPI.PRIMARY_GOAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BayesianHelperTest tests.
 *
 * @author vico
 */
public class BayesianHelperTest extends UnitTestBase {

    private static final String DEFAULT_VARIANT = "DEFAULT";
    private static final String TEST_VARIANT = "test";

    private ExperimentResults experimentResults;
    private GoalResults goalResults;
    private VariantResults variantResultsA;
    private VariantResults variantResultsB;

    @Before
    public void setup() {
        experimentResults = mock(ExperimentResults.class);
        goalResults = mock(GoalResults.class);
        ExperimentResults.TotalSession totalSession = mock(ExperimentResults.TotalSession.class);
        when(experimentResults.getGoals()).thenReturn(Map.of(PRIMARY_GOAL, goalResults));
        when(experimentResults.getSessions()).thenReturn(totalSession);
        when(totalSession.getTotal()).thenReturn(120L);
        when(totalSession.getVariants()).thenReturn(Map.of(
            DEFAULT_VARIANT, 60L,
            TEST_VARIANT, 60L
        ));
        variantResultsA = mock(VariantResults.class);
        variantResultsB = mock(VariantResults.class);
        when(goalResults.getVariants()).thenReturn(Map.of(
            DEFAULT_VARIANT, variantResultsA,
            TEST_VARIANT, variantResultsB
        ));
        VariantResults.UniqueBySessionResume uniqueBySessionResumeA = mock(VariantResults.UniqueBySessionResume.class);
        VariantResults.UniqueBySessionResume uniqueBySessionResumeB = mock(VariantResults.UniqueBySessionResume.class);
        when(variantResultsA.getUniqueBySession()).thenReturn(uniqueBySessionResumeA);
        when(variantResultsA.getVariantName()).thenReturn(DEFAULT_VARIANT);
        when(variantResultsB.getVariantName()).thenReturn(TEST_VARIANT);
        when(variantResultsB.getUniqueBySession()).thenReturn(uniqueBySessionResumeB);
        when(uniqueBySessionResumeA.getCount()).thenReturn(16L);
        when(uniqueBySessionResumeB.getCount()).thenReturn(50L);
    }


    /**
     * Method to: Test {@link BayesianHelper#toBayesianInput(ExperimentResults, String)}
     *
     * Given Scenario: A valid experiment results
     * ExpectedResult: A valid BayesianInput
     */
    @Test
    public void test_toBayesianInput() {
        final BayesianInput bayesianInput = BayesianHelper.get().toBayesianInput(experimentResults, PRIMARY_GOAL);
        assertSame(ABTestingType.AB, bayesianInput.type());
        assertEquals(DEFAULT_VARIANT, bayesianInput.control().variant());
        assertEquals(16L, bayesianInput.control().successes());
        assertEquals(44L, bayesianInput.control().failures());
        assertEquals(1, bayesianInput.variantPairs().size());
        final VariantInput testInputPair = bayesianInput.variantPairs().get(0);
        assertEquals(TEST_VARIANT, testInputPair.variant());
        assertEquals(50L, testInputPair.successes());
        assertEquals(10L, testInputPair.failures());
    }

    /**
     * Method to: Test {@link BayesianHelper#toBayesianInput(ExperimentResults, String)}
     *
     * Given Scenario: A valid experiment results with no goals
     * ExpectedResult: A null BayesianInput
     */
    @Test
    public void test_toBayesianInput_noDefault() {
        when(goalResults.getVariants()).thenReturn(Map.of(
            TEST_VARIANT, variantResultsB
        ));

        final BayesianInput bayesianInput = BayesianHelper.get().toBayesianInput(experimentResults, PRIMARY_GOAL);
        assertNull(bayesianInput);
    }

    /**
     * Method to: Test {@link BayesianHelper#toBayesianInput(ExperimentResults, String)}
     *
     * Given Scenario: A valid experiment results with no goals
     * ExpectedResult: A null BayesianInput
     */
    @Test
    public void test_toBayesianInput_noTest() {
        when(goalResults.getVariants()).thenReturn(Map.of(
            DEFAULT_VARIANT, variantResultsA
        ));

        final BayesianInput bayesianInput = BayesianHelper.get().toBayesianInput(experimentResults, PRIMARY_GOAL);
        assertNull(bayesianInput);
    }

}
