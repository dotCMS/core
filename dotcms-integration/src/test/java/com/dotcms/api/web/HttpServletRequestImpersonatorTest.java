package com.dotcms.api.web;

import static org.mockito.Mockito.mock;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link HttpServletRequestImpersonator}
 */
public class HttpServletRequestImpersonatorTest {

    static final HttpServletRequestImpersonator test = new HttpServletRequestImpersonator();

    /**
     * Test method for {@link HttpServletRequestImpersonator#isMockRequest(HttpServletRequest)}.
     * Given Scenario: A request is passed to the method.
     * Expected: The method should return true if the request is a mock request. Only the
     */
    @Test
    public void testIsMockRequest_identifiesMockRequests() {

        final HttpServletRequest mockHeaderRequest = new MockHeaderRequest(new FakeHttpRequest("localhost", "/").request());
        final HttpServletRequest mockSessionRequest = mock(HttpServletRequest.class);
        final HttpServletRequest localRequest = new FakeHttpRequest("localhost", "/").request();

        Assert.assertTrue(test.isMockRequest(mockHeaderRequest));
        Assert.assertTrue(test.isMockRequest(mockSessionRequest));
        Assert.assertTrue(test.isMockRequest(localRequest));

        final HttpServletRequest wrapper = new HttpServletRequestWrapper(mockHeaderRequest);
        Assert.assertFalse(test.isMockRequest(wrapper));
    }

}
