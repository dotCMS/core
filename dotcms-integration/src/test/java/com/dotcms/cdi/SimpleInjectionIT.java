package com.dotcms.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for simple CDI injection
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class SimpleInjectionIT  {

    /**
     * Test CDI injection
     * Given scenario: An annotated bean is injected
     * Expected result: the bean is injected
     */
    @Test
    public void testInjection() {
        final GreetingBean greetingBean = CDIUtils.getBeanThrows(GreetingBean.class);
        assertEquals("Hello World", greetingBean.greet());
    }

}
