package com.dotcms.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import java.util.Optional;
import org.junit.Test;

/**
 * Integration test for simple CDI injection
 */
public class SimpleInjectionIT extends IntegrationTestBase {

    /**
     * Test CDI injection
     * Given scenario: An annotated bean is injected
     * Expected result: the bean is injected
     */
    @Test
    public void testInjection() {
        Optional<SayHelloBean> sayHelloBean = CDIUtils.getBean(SayHelloBean.class);
        assertTrue(sayHelloBean.isPresent());
        assertEquals("Hello", sayHelloBean.get().sayHello());
    }

}
