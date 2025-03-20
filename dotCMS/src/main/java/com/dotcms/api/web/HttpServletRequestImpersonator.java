package com.dotcms.api.web;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * The Request impersonator is a class that will return a Mock instance of a request object.
 * We use this to block the real request object from being used to perform operations that could break certain flows
 * Sometimes we want to block a session invalidation for example or a redirect, so we use this class to return a fake request object.
 */
public class HttpServletRequestImpersonator {

    private static final Pattern MOCK_OR_FAKE_PATTERN = Pattern.compile("(^|\\b|\\.)mock|fake($|\\b|\\.)", Pattern.CASE_INSENSITIVE);

    /**
     * new instance of {@link HttpServletRequestImpersonator}
     * @return {@link HttpServletRequestImpersonator}
     */
    public static HttpServletRequestImpersonator newInstance() {
        return new HttpServletRequestImpersonator();
    }

    /**
     * Returns a fake request object
     * @return {@link HttpServletRequest}
     */
    public HttpServletRequest request() {
        final HttpServletRequest request =  HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (isMockRequest(request)) {
            // no use in mocking a mock this could actually break tests
            return request;
        }
        return request == null
                ? new FakeHttpRequest("localhost", "/").request()
                : new MockHeaderRequest(new MockAttributeRequest(new MockSessionRequest(new MockParameterRequest(request))));
    }

    /**
     * Returns a fake response object
     * @return {@link HttpServletResponse}
     */
    public HttpServletResponse response() {
        return new MockHttpResponse();
    }

    /**
     * Check if the request is a mock request
     * as We have so many different types of mock requests this is probably the best way to check
     * @param request {@link HttpServletRequest}
     * @return boolean
     */
    boolean isMockRequest(final HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        final String clazzName = request.getClass().getName();
        return MOCK_OR_FAKE_PATTERN.matcher(clazzName).find();
    }

}
