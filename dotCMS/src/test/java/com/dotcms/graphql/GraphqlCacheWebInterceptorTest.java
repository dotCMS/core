package com.dotcms.graphql;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for the TTL handling in {@link GraphqlCacheWebInterceptor}.
 */
public class GraphqlCacheWebInterceptorTest {

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: a requested TTL above the configured maximum
     * ExpectedResult: the maximum is applied
     */
    @Test
    public void capTTL_capsValuesAboveTheMaximum() {
        assertEquals(3600, GraphqlCacheWebInterceptor.capTTL(999999, 3600));
        assertEquals(3600, GraphqlCacheWebInterceptor.capTTL(3601, 3600));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: a requested TTL at or below the configured maximum
     * ExpectedResult: the requested value is kept
     */
    @Test
    public void capTTL_keepsValuesWithinTheMaximum() {
        assertEquals(15, GraphqlCacheWebInterceptor.capTTL(15, 3600));
        assertEquals(3600, GraphqlCacheWebInterceptor.capTTL(3600, 3600));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: the bypass (0) and invalidate (-1) values
     * ExpectedResult: they are returned unchanged so they keep their meaning
     */
    @Test
    public void capTTL_keepsSpecialValues() {
        assertEquals(0, GraphqlCacheWebInterceptor.capTTL(0, 3600));
        assertEquals(GraphqlCacheWebInterceptor.INVALIDATE_CACHE,
                GraphqlCacheWebInterceptor.capTTL(GraphqlCacheWebInterceptor.INVALIDATE_CACHE, 3600));
    }

    /**
     * Method to test: {@link GraphqlCacheWebInterceptor#capTTL(int, int)}
     * Given Scenario: a non-positive maximum
     * ExpectedResult: no cap is applied
     */
    @Test
    public void capTTL_nonPositiveMaximumDisablesTheCap() {
        assertEquals(999999, GraphqlCacheWebInterceptor.capTTL(999999, 0));
        assertEquals(999999, GraphqlCacheWebInterceptor.capTTL(999999, -1));
    }
}
