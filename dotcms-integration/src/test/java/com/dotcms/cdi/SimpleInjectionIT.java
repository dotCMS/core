package com.dotcms.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import com.dotcms.IntegrationTestBase;

public class SimpleInjectionIT extends IntegrationTestBase {

    @Test
    public void testInjection() {
        SayHelloBean sayHelloBean = CDIUtils.getBean(SayHelloBean.class);
        assertNotNull(sayHelloBean);
        assertEquals("Hello", sayHelloBean.sayHello());
    }

}
