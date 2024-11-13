package com.dotcms.cdi;

import static org.junit.Assert.assertEquals;

import com.dotcms.DataProviderWeldRunner;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for simple CDI injection using the Runner DataProviderWeldRunner
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class SimpleDataProviderWeldRunnerInjectionIT {

    @Inject
    GreetingBean greetingBean;

    /**
     * Test that DataProviderWeldRunner can inject a bean and receive a value from a data provider
     * @param testCase the test case
     */
    @UseDataProvider("testCases")
    @Test
    public void testInjection(TestCase testCase) {
        assertEquals("lol", testCase.getValue());
        assertEquals ("Hello World", greetingBean.greet());
    }

    @DataProvider
    public static Object[] testCases() {
        return new Object[]{
                new TestCase("lol"),
        };
    }

    public static class TestCase {
        private final String value;
        public TestCase(String value){
            this.value = value;
        }
        public String getValue(){
            return value;
        }
    }

}
