package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigUtilsTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test method {@link ConfigUtils#isFeatureFlagOn(String)}
     * Given scenario: A feature flag is explicitly set to true
     * Expected result: the method should return true
     */
    @Test
    public void test_isFeatureFlagOn_when_flag_is_true_should_return_true(){
        assertTrue(ConfigUtils.isFeatureFlagOn("FEATURE_FLAG_DUMMY_TRUE"));
    }

    /**
     * Test method {@link ConfigUtils#isFeatureFlagOn(String)}
     * Given scenario: A feature flag is explicitly set to false
     * Expected result: the method should return false
     */
    @Test
    public void test_isFeatureFlagOn_when_flag_is_false_should_return_false(){
        assertFalse(ConfigUtils.isFeatureFlagOn("FEATURE_FLAG_DUMMY_FALSE"));
    }

    /**
     * Test method {@link ConfigUtils#isFeatureFlagOn(String)}
     * Given scenario: A feature flag is not set
     * Expected result: the method should return true
     */
    @Test
    public void test_isFeatureFlagOn_when_flag_is_null_should_return_true(){
        assertTrue(ConfigUtils.isFeatureFlagOn("FEATURE_FLAG_DUMMY"));
    }
}
