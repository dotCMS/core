package com.dotcms.cdi;

import static org.junit.Assert.assertEquals;

import com.dotcms.JUnit4WeldRunner;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;

@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class SimpleJUnit4InjectionIT {

    @Inject
    GreetingBean greetingBean;

    /**
     * Test CDI injection using the Runner JUnit4WeldRunner
     */
    @Test
    public void testInjection() {
        final String greet = greetingBean.greet();
        assertEquals("Hello World", greet);
    }
}
