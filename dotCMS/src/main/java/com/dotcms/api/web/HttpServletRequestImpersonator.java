package com.dotcms.api.web;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpServletRequestImpersonator {

    public static HttpServletRequestImpersonator newInstance() {
        return new HttpServletRequestImpersonator();
    }

    public HttpServletRequest request() {
        final HttpServletRequest request =  HttpServletRequestThreadLocal.INSTANCE.getRequest();
        return request == null
                ? new FakeHttpRequest("localhost", "/").request()
                : new MockHeaderRequest(new MockAttributeRequest(new MockSessionRequest(new MockParameterRequest(request))));
    }

    public HttpServletResponse response() {
        return new MockHttpResponse();
    }

}
