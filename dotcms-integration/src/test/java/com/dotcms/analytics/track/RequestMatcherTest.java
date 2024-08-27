package com.dotcms.analytics.track;

import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for RequestMatcher
 */
public class RequestMatcherTest {

    @BeforeClass
    public static void beforeClass() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Will use a default implementation of the method (without implementing anything)
     * ExpectedResult: Any of the matches wont work, so the method will return false
     */
    @Test
    public void test_default_implementation() throws Exception {

        final RequestMatcher requestMatcher = new RequestMatcher() {
            // empty implementation
        };
        final FakeHttpRequest request = new FakeHttpRequest("localhost", "/test");
        final boolean result = requestMatcher.match(request.request(), null);
        assertFalse(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher for the exact uri path and the GET method
     * ExpectedResult: Since both criteria are met, the method will return true
     */
    @Test
    public void test_get_method_with_valid_exact_match() throws Exception {

        final String url = "/test";
        final RequestMatcher requestMatcher = new RequestMatcher() {

            @Override
            public Set<String> getMatcherPatterns() {
                return Set.of(url);
            }

            @Override
            public Set<String> getAllowedMethods() {
                return Set.of(HttpMethod.GET);
            }
        };

        final DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/api/v1/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost" + url));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);
        final boolean result = requestMatcher.match(mockReq);
        assertTrue(result);
    }

}
