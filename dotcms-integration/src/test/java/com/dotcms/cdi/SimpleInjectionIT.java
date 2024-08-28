package com.dotcms.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import java.util.Optional;
import org.junit.Test;

public class SimpleInjectionIT extends IntegrationTestBase {

    @Test
    public void testInjection() {
        Optional<SayHelloBean> sayHelloBean = CDIUtils.getBean(SayHelloBean.class);
        assertTrue(sayHelloBean.isPresent());
        assertEquals("Hello", sayHelloBean.get().sayHello());
    }

}
