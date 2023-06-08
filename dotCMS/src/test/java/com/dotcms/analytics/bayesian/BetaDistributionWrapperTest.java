package com.dotcms.analytics.bayesian;

import com.dotcms.analytics.bayesian.beta.BetaDistributionWrapper;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * DotBetaDistribution unit test.
 *
 * @author vico
 */
public class BetaDistributionWrapperTest {

    private BetaDistributionWrapper distribution;

    @Before
    public void setup() {
        distribution = BetaDistributionWrapper.create(10, 10);
    }

    /**
     * Given a defined number of elements to calculate to simulate graph points in a two dimensions chart,
     * verify that its returned values are within the expected parameters.
     * Values to compare were taken from Bayesian calculator we based ou work from:
     * <a href="https://making.lyst.com/bayesian-calculator/">...</a>
     */
    @Test
    public void test_pdfElements() {
        final var limit = 1000;
        final var graphValues = IntStream
                .range(0, limit)
                .mapToObj(operand -> {
                    final double x = (double) operand / limit;
                    final double temp = distribution.pdf(x);
                    final double y = temp == Double.POSITIVE_INFINITY ? 0 : temp;
                    return new Tuple2<>(x, y);
                })
                .collect(Collectors.toList());
        Assert.assertEquals(0.0, graphValues.get(0)._1, 0.0);
        Assert.assertEquals(0.0, graphValues.get(0)._2, 0.0);
        Assert.assertEquals(0.5, graphValues.get(500)._1, 0.0);
        Assert.assertEquals(3.523941040071157, graphValues.get(500)._2, 0.000000000100000);
        Assert.assertEquals(0.999, graphValues.get(999)._1, 0.0);
        Assert.assertEquals(9.154991586071056e-22, graphValues.get(999)._2, 0.000000000100000);
    }

}
