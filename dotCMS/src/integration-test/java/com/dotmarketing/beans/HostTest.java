package com.dotmarketing.beans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class HostTest {

    @BeforeClass
    public static void initData() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to test:</b> {@link Host#isDefault()}<p></p>
     * <b>Test Case:</b> When the method is called on an object without setting the property {@link Host#IS_DEFAULT_KEY}, it should return false<p></p>
     * <b>Expected Results:</b> When the method {@link Host#isDefault()} is invoked, it should return false
     */
    @Test
    public void isDefaultReturnsFalseWhenNotSet(){
        Host host = new Host();
        assertFalse(host.isDefault());
    }

    /**
     * <b>Method to test:</b> {@link Host#isDefault()}<p></p>
     * <b>Test Case:</b> When the property {@link Host#IS_DEFAULT_KEY} is set, it should return a proper value<p></p>
     * <b>Expected Results:</b> When the method {@link Host#isDefault()} is invoked, it should return the value set in {@link Host#IS_DEFAULT_KEY}
     */
    @Test
    public void isDefaultReturnsProperValueWhenSet(){
        Host host = new Host();
        host.setDefault(true);
        assertTrue(host.isDefault());

        host.setDefault(false);
        assertFalse(host.isDefault());
    }

}
