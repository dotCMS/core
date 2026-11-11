package com.dotcms.auth;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link InteractiveIdentityFilter}: it hands the wrapped request down the chain, makes the
 * thread-bound request the wrapped one, and is registered before the interceptor filter.
 */
public class InteractiveIdentityFilterTest {

    @After
    public void cleanup() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * The chain receives a wrapped request, and the thread-bound request is that same object.
     */
    @Test
    public void test_chain_gets_the_wrapped_request_and_the_thread_local_follows() throws Exception {
        final HttpServletRequest container = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(container);
        final AtomicReference<Object> seenByChain = new AtomicReference<>();
        final AtomicReference<Object> seenInThreadLocal = new AtomicReference<>();
        final FilterChain chain = mock(FilterChain.class);
        doAnswer(i -> {
            seenByChain.set(i.getArgument(0));
            seenInThreadLocal.set(HttpServletRequestThreadLocal.INSTANCE.getRequest());
            return null;
        }).when(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        new InteractiveIdentityFilter().doFilter(container, mock(HttpServletResponse.class), chain);

        assertNotNull(seenByChain.get());
        assertNotSame(container, seenByChain.get());
        assertSame(seenByChain.get(), seenInThreadLocal.get());
    }

    /**
     * The mapping in web.xml places the filter before the interceptor filter.
     */
    @Test
    public void test_filter_is_mapped_before_the_interceptor_filter() throws Exception {
        final Document web = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File(new File(InteractiveIdentityFilterTest.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI()), "../../src/main/webapp/WEB-INF/web.xml").getCanonicalFile());
        final NodeList mappings = web.getElementsByTagName("filter-mapping");
        int identity = -1;
        int interceptor = -1;
        for (int i = 0; i < mappings.getLength(); i++) {
            final String name = ((org.w3c.dom.Element) mappings.item(i))
                    .getElementsByTagName("filter-name").item(0).getTextContent().trim();
            if ("InteractiveIdentityFilter".equals(name)) {
                identity = i;
            } else if ("InterceptorFilter".equals(name)) {
                interceptor = i;
            }
        }
        assertTrue("filter is not mapped", identity >= 0);
        assertTrue("filter must run before InterceptorFilter", identity < interceptor);
    }
}
