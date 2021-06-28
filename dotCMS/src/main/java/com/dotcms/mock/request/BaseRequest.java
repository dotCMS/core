package com.dotcms.mock.request;


import javax.servlet.http.HttpServletRequest;

/**
 * Primary call to {@link Mockito} in order to create the {@link HttpServletRequest}
 *
 * See an example here: {@link MockHttpRequest#MockHttpRequest(String, String)}
 */
public class BaseRequest implements MockRequest {

    public HttpServletRequest request() {
        return new DotCMSMockRequest();
    }
}
